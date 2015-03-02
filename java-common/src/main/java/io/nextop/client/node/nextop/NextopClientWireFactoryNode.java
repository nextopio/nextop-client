package io.nextop.client.node.nextop;

import io.nextop.*;
import io.nextop.client.MessageControl;
import io.nextop.client.Wire;
import io.nextop.client.Wires;
import io.nextop.client.node.AbstractMessageControlNode;
import io.nextop.client.node.Head;
import io.nextop.client.node.http.HttpNode;
import io.nextop.client.retry.SendStrategy;
import io.nextop.org.apache.http.HttpStatus;

import javax.annotation.Nullable;
import javax.net.SocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

// FIXME(security) client TLS certificate. the certificate is used to verify the client ID
// FIXME node implementation
public class NextopClientWireFactoryNode extends AbstractMessageControlNode implements Wire.Factory {

    public static final class Config {
        final int allowedFailsPerAuthority;


        Config(int allowedFailsPerAuthority) {
            this.allowedFailsPerAuthority = allowedFailsPerAuthority;
        }
    }

    static final Config DEFAULT_CONFIG = new Config(2);



    static final SendStrategy DEFAULT_DNS_SEND_STRATEGY = new SendStrategy.Builder()
            .withUniformRandom(2000, TimeUnit.MILLISECONDS)
            .build();
    static final SendStrategy FAILSAFE_DNS_SEND_STRATEGY = DEFAULT_DNS_SEND_STRATEGY;

    // exponential backoff to a long poll
    static final SendStrategy DEFAULT_DNS_RETAKE_STRATEGY = new SendStrategy.Builder()
            .init(2000, TimeUnit.MILLISECONDS)
            .withExponentialRandom(1.1f)
            // FIXME really want a repeatUntil(200s)
            .repeat(50)
            .withUniformRandom(300, TimeUnit.SECONDS)
            .build();
    static final SendStrategy FAILSAFE_DNS_RETAKE_STRATEGY = DEFAULT_DNS_RETAKE_STRATEGY;



    final Config config;

    // FIXME want to save this state, so that when the node comes back,
    // FIXME it doesn't have to hit DNS to get active
    final State state;

//    NextopNode nextopNode;


    // this should be an aggressive uniform poll
    SendStrategy dnsSendStrategy;
    // this should be an exponential backoff up to a long poll
    SendStrategy dnsRetakeStrategy;


    SendStrategy mostRecentDnsSendStrategy = null;
    long mostRecentDnsSendNanos = 0L;
    SendStrategy mostRecentDnsRetakeStrategy = null;


    SocketFactory socketFactory;


    byte[] greetingBuffer = new byte[1024];



    final HttpNode dnsHttpNode;
    final Head dnsHead;


    // FIXME
    boolean active;

    Id clientId;
    // FIXME
    Id accessKey = Id.create();
    Set<Id> grantKeys = Collections.emptySet();



    public NextopClientWireFactoryNode() {
        this(DEFAULT_CONFIG, new State());
    }
    public NextopClientWireFactoryNode(Config config, State state) {
        this.config = config;
        this.state = state;


        dnsHttpNode = new HttpNode();
        dnsHead = Head.create(this, getMessageControlState(), dnsHttpNode, getScheduler());
    }


    @Override
    public void onActive(boolean active) {
        // FIXME
    }

    @Override
    public void onMessageControl(MessageControl mc) {
        // FIXME
    }


    // if state has all failed endpoints,
    // post failed to DNS
    // query DNS for new endpoints
    // keep doing that for n tries
    // if connection is up, then return wire
    // else not available

    // FIXME this should do tls. once handshake an establish an intro (send access key, etc), then

    /////// Wire.Factory IMPLEMENTATION ///////

    @Override
    public Wire create(@Nullable Wire replace) throws NoSuchElementException {

        // if replace, mark replaced authority as failed

        // while there is an up authority
        // attempt to connect
        // if successful, mark success, (TODO upgrade to tls), return
        // if failed, repeat

        // (retry timeout for dns requests, to avoid ddossing the dns)
        // at this point there are no more up authorities
        // send a dns request for more authorities
        // repeat top

        if (replace instanceof NextopRemoteWire) {
            state.fail(((NextopRemoteWire) replace).authority);
        }


        top:
        while (active) {
            @Nullable Authority upAuthority;
            try {
                mostRecentDnsRetakeStrategy = dnsRetakeStrategy;
                if (null == mostRecentDnsSendStrategy) {
                    mostRecentDnsSendStrategy = dnsSendStrategy;
                }
                while (null == (upAuthority = state.getFirstUpAuthority(config.allowedFailsPerAuthority))) {
                    if (!active) {
                        continue top;
                    }

                    doDnsSendDelay();
                    if (!doDnsReset()) {
                        // there was an error with dns
                        doDnsRetakeDelay();
                    } else {
                        mostRecentDnsRetakeStrategy = dnsRetakeStrategy;
                    }
                    mostRecentDnsSendNanos = System.nanoTime();
                }
            } catch (Exception e) {
                continue top;
            }

            assert null != upAuthority;
            try {
                Socket socket = socketFactory.createSocket(Authority.toInetAddress(upAuthority), upAuthority.port);

                writeGreeting(socket.getOutputStream());
                readGreetingResponse(socket.getInputStream());

                Socket tlsSocket = startTls(socket);

                state.success(upAuthority);
                return Wires.io(tlsSocket.getInputStream(), tlsSocket.getOutputStream());
            } catch (Exception e) {
                // FIXME work out the case where this was a network outage
                state.fail(upAuthority);
                continue top;
            }
        }

        throw new NoSuchElementException();
    }

    void doDnsSendDelay() throws InterruptedException {
        if (0 < mostRecentDnsSendNanos) {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - mostRecentDnsSendNanos);

            mostRecentDnsSendStrategy = mostRecentDnsSendStrategy.retry();
            if (!mostRecentDnsSendStrategy.isSend()) {
                mostRecentDnsSendStrategy = FAILSAFE_DNS_SEND_STRATEGY.retry();
            }
            assert mostRecentDnsSendStrategy.isSend();

            long delayMs = mostRecentDnsSendStrategy.getDelay(TimeUnit.MILLISECONDS);
            if (elapsedMs < delayMs) {
                Thread.sleep(delayMs - elapsedMs);
            }
        }
    }

    void doDnsRetakeDelay() throws InterruptedException {
        mostRecentDnsRetakeStrategy = mostRecentDnsRetakeStrategy.retry();
        if (!mostRecentDnsRetakeStrategy.isSend()) {
            mostRecentDnsRetakeStrategy = FAILSAFE_DNS_RETAKE_STRATEGY.retry();
        }
        assert mostRecentDnsRetakeStrategy.isSend();

        long delayMs = mostRecentDnsSendStrategy.getDelay(TimeUnit.MILLISECONDS);
        if (0 < delayMs) {
            Thread.sleep(delayMs);
        }
    }

    boolean doDnsReset() {
        Authority dnsAuthority = Authority.valueOf("54.149.233.13:2778");
        // FIXME "dns.nextop.io" in prod

        List<Authority> reportDownAuthorities = state.getUnreportedDownAuthorities(config.allowedFailsPerAuthority);
        Message dnsRequest;
        if (reportDownAuthorities.isEmpty()) {
            dnsRequest = Message.newBuilder()
                    .setRoute(Route.valueOf("GET http://" + dnsAuthority + "/$access-key/edge.json"))
                    .set("access-key", accessKey)
                    .build();
        } else {
            // report the down authorities
            List<String> reportDownAuthorityStrings = new ArrayList<String>(reportDownAuthorities.size());
            for (Authority reportDownAuthority : reportDownAuthorities) {
                reportDownAuthorityStrings.add(reportDownAuthority.toString());
            }

            dnsRequest = Message.newBuilder()
                    .setRoute(Route.valueOf("POST http://" + dnsAuthority + "/$access-key/edge.json"))
                    .set("access-key", accessKey)
                    .set("bad-authorities", WireValue.of(reportDownAuthorityStrings))
                    .build();
        }

        Message dnsResponse;
        try {
            dnsHead.send(dnsRequest);
            dnsResponse = dnsHead.receive(dnsRequest.inboxRoute()).toBlocking().single();
        } catch (Exception e) {
            // FIXME log
            dnsHead.cancelSend(dnsRequest.id);
            return false;
        }

        if (HttpStatus.SC_OK != dnsResponse.getCode()) {
            return false;
        } else {
            // mark the unreported down as reported
            for (Authority reportDownAuthority : reportDownAuthorities) {
                state.setReportedDown(reportDownAuthority);
            }

            try {
                @Nullable WireValue contentValue = dnsResponse.getContent();
                if (null != contentValue) {
                    @Nullable WireValue authoritiesValue = contentValue.asMap().get(WireValue.of("authorities"));
                    if (null != authoritiesValue) {
                        List<WireValue> dnsAuthorityValues = authoritiesValue.asList();
                        List<Authority> dnsAuthorities = new ArrayList<Authority>(dnsAuthorityValues.size());
                        for (WireValue dnsAuthorityValue : dnsAuthorityValues) {
                            dnsAuthorities.add(Authority.valueOf(dnsAuthority.toString()));
                        }

                        state.resetDnsAuthorities(dnsAuthorities);
                        return true;
                    }
                }
            } catch (Exception e) {
                // FIXME log
                // fall through
            }
            return false;
        }
    }

    void writeGreeting(OutputStream os) throws IOException {
        // FIXME send the client ID (each client has a unique ID that is used for reconnects)
        // FIXME send the client certificate for TLS
        Message greeting = Message.newBuilder()
                .set("accessKey", accessKey)
                .set("grantKeys", WireValue.of(grantKeys))
                .set("clientId", clientId)
                .build();

        ByteBuffer bb = ByteBuffer.wrap(greetingBuffer, 2, greetingBuffer.length - 2);
        WireValue.of(greeting).toBytes(bb);
        bb.flip();

        int length = bb.remaining();
        greetingBuffer[0] = (byte) (length >>> 8);
        greetingBuffer[1] = (byte) length;

        os.write(greetingBuffer, 0, 2 + bb.remaining());
        os.flush();
    }

    void readGreetingResponse(InputStream is) throws IOException {
        int i = 0;
        for (int r; 0 < (r = is.read(greetingBuffer, i, 2 - i)); ) {
            i += r;
        }
        if (i < 2) {
            throw new IOException();
        }

        int length = ((0xFF & greetingBuffer[0]) << 8) | (0xFF & greetingBuffer[1]);
        if (greetingBuffer.length < length) {
            throw new IOException("Greeting response too long.");
        }

        i = 0;
        for (int r; 0 < (r = is.read(greetingBuffer, i, length - i)); ) {
            i += r;
        }
        if (i < length) {
            throw new IOException();
        }

        WireValue responseValue = WireValue.valueOf(greetingBuffer);
        switch (responseValue.getType()) {
            case MESSAGE:
                handleGreetingResponse(responseValue.asMessage());
                break;
            default:
                throw new IOException("Bad greeting response.");
        }
    }
    void handleGreetingResponse(Message response) {
        // FIXME sessionId
    }

    /** starts a TLS session on the socket. blocks until the handshake is completed,
     * with certificates exchanged and verified. */
    Socket startTls(Socket socket) throws IOException {
        // FIXME
        return socket;
    }


    static final class NextopRemoteWire implements Wire {
        final Wire impl;
        final Authority authority;


        NextopRemoteWire(Wire impl, Authority authority) {
            this.impl = impl;
            this.authority = authority;
        }

        @Override
        public void close() throws IOException {
            impl.close();
        }

        @Override
        public void read(byte[] buffer, int offset, int length, int messageBoundary) throws IOException {
            impl.read(buffer, offset, length, messageBoundary);
        }

        @Override
        public void skip(long n, int messageBoundary) throws IOException {
            impl.skip(n, messageBoundary);
        }

        @Override
        public void write(byte[] buffer, int offset, int n, int messageBoundary) throws IOException {
            impl.write(buffer, offset, n, messageBoundary);
        }

        @Override
        public void flush() throws IOException {
            impl.flush();
        }
    }




    public static final class State implements Serializable {


        List<AuthorityState> authorityStates = Collections.emptyList();
        Map<Authority, AuthorityState> allAuthorityStates = new HashMap<Authority, AuthorityState>(8);


        State() {
        }


        @Nullable
        Authority getFirstUpAuthority(int allowedFailsPerAuthority) {
            for (AuthorityState authorityState : authorityStates) {
                if (!authorityState.isDown(allowedFailsPerAuthority)) {
                    return authorityState.authority;
                }
            }
            return null;
        }

        List<Authority> getUnreportedDownAuthorities(int allowedFailsPerAuthority) {
            List<Authority> unreportedDownAuthorities = new LinkedList<Authority>();
            for (AuthorityState authorityState : authorityStates) {
                if (authorityState.isDown(allowedFailsPerAuthority) && !authorityState.reportedDown) {
                    unreportedDownAuthorities.add(authorityState.authority);
                }
            }
            return unreportedDownAuthorities;
        }


        void resetDnsAuthorities(List<Authority> dnsAuthorities) {
            // resolve with states
            List<AuthorityState> dnsAuthorityStates = new ArrayList<AuthorityState>(dnsAuthorities.size());
            for (Authority authority : dnsAuthorities) {
                AuthorityState authorityState = allAuthorityStates.get(authority);
                if (null == authorityState) {
                    authorityState = new AuthorityState(authority);
                    allAuthorityStates.put(authority, authorityState);
                }
                // mark the state with a dns reset
                authorityState.addAttempt(AuthorityState.Attempt.create(AuthorityState.Attempt.Type.DNS_RESET));
                dnsAuthorityStates.add(authorityState);
            }
            authorityStates = dnsAuthorityStates;
        }

        void success(Authority authority) {
            @Nullable AuthorityState authorityState = allAuthorityStates.get(authority);
            assert null != authorityState;
            if (null != authorityState) {
                authorityState.addAttempt(AuthorityState.Attempt.create(AuthorityState.Attempt.Type.SUCCESS));
            }
        }

        void fail(Authority authority) {
            @Nullable AuthorityState authorityState = allAuthorityStates.get(authority);
            assert null != authorityState;
            if (null != authorityState) {
                authorityState.addAttempt(AuthorityState.Attempt.create(AuthorityState.Attempt.Type.FAIL));
            }
        }

        void setReportedDown(Authority authority) {
            @Nullable AuthorityState authorityState = allAuthorityStates.get(authority);
            assert null != authorityState;
            if (null != authorityState) {
                authorityState.reportedDown = true;
            }
        }



        // FIXME serializable
        static final class AuthorityState {
            final Authority authority;
            final Attempt[] attempts = new Attempt[16];
            int attemptNextIndex = 0;
            int attemptCount = 0;

            boolean reportedDown = false;


            AuthorityState(Authority authority) {
                this.authority = authority;
            }


            void addAttempt(Attempt attempt) {
                int n = attempts.length;
                if (attemptCount < n) {
                    attemptCount += 1;
                }
                attempts[attemptNextIndex] = attempt;
                attemptNextIndex = (attemptNextIndex + 1) % n;
            }


            // don't try again on a most recently failed
            boolean isMostRecentlyFailed() {
                int n = attempts.length;
                int attemptIndex = ((attemptNextIndex - 1) + n) % n;
                switch (attempts[attemptIndex].type) {
                    case FAIL:
                        return true;
                    default:
                        return false;
                }
            }

            // if the two most recent requests have failed, then consider this down
            boolean isDown(int allowedFails) {
                int n = attempts.length;
                if (attemptCount < allowedFails) {
                    return false;
                } else {
                    for (int i = 0; i < allowedFails; ++i) {
                        int attemptIndex = ((attemptNextIndex - 1 - i) + n) % n;
                        switch (attempts[attemptIndex].type) {
                            case FAIL:
                                // continue
                                break;
                            default:
                                return false;
                        }
                    }
                    return true;
                }
            }



            static final class Attempt {
                static enum Type {
                    SUCCESS,
                    FAIL,
                    DNS_RESET
                }

                static Attempt create(Type type) {
                    return new Attempt(type, System.currentTimeMillis());
                }

                final Type type;
                final long time;

                Attempt(Type type, long time) {
                    this.type = type;
                    this.time = time;
                }
            }
        }
    }
}
