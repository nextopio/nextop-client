package io.nextop.client.node;


import com.google.common.collect.ImmutableSet;
import io.nextop.Authority;
import io.nextop.client.MessageControl;
import io.nextop.client.MessageControlChannel;
import io.nextop.client.MessageControlNode;
import io.nextop.client.MessageControlState;
import io.nextop.log.NL;
import rx.Scheduler;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.*;

// multi down

// this node maintains multiple (n) downstream nodes ranked by preference
// routes control to the highest pref downsteam that is active
// when the active state of a downstream changes, this node ensures that the active state of the highest downstream is exclusively set
public class MultiNode extends AbstractMessageControlNode {

    private final DownstreamState[] downstreamStates;

    private Queue<MessageControl> pendingMessageControls = new LinkedList<MessageControl>();

    private boolean active = false;

    private Collection<Subnet> localSubnets = Collections.emptyList();


    /** @param downstreams the order is the preference (0 top) */
    public MultiNode(Downstream ... downstreams) {
        int n = downstreams.length;
        downstreamStates = new DownstreamState[n];
        for (int i = 0; i < n; ++i) {
            downstreamStates[i] = new DownstreamState(downstreams[i], false);
        }
    }


    @Nullable
    private MessageControlNode getActiveDownstream() {
        for (DownstreamState state : downstreamStates) {
            if (state.downActive) {
                assert state.upActive;
                return state.downstream.node;
            }
        }
        return null;
    }

    private void setActiveDownstream() {
        // scan the down states for the first with upActive and compatible
        // if there is another set downActive, unset that, then set the new

        if (!active) {
            clearActiveDownstream();
        } else {

            int firstUpActiveIndex = -1;
            for (int i = 0, n = downstreamStates.length; i < n; ++i) {
                DownstreamState state = downstreamStates[i];
                if (state.upActive && state.compatible) {
                    firstUpActiveIndex = i;
                    break;
                }
            }

            if (firstUpActiveIndex < 0) {
                clearActiveDownstream();
            } else if (!downstreamStates[firstUpActiveIndex].downActive) {
                // unset
                clearActiveDownstream();

                // set
                DownstreamState activeDownstreamState = downstreamStates[firstUpActiveIndex];
                activeDownstreamState.downActive = true;
                activeDownstreamState.downstream.node.onActive(true);
                for (@Nullable MessageControl mc; null != (mc = pendingMessageControls.poll()); ) {
                    activeDownstreamState.downstream.node.onMessageControl(mc);
                }
            }
        }
    }

    private void clearActiveDownstream() {
        for (int i = 0, n = downstreamStates.length; i < n; ++i) {
            DownstreamState state = downstreamStates[i];
            if (state.downActive) {
                state.downstream.node.onActive(false);
                break;
            }
        }
        assert null == getActiveDownstream();
    }


    @Override
    protected void initSelf(@Nullable Bundle savedState) {
        upstream.onActive(true);

        try {
            localSubnets = findLocalSubnets();
        } catch (IOException e) {
            NL.nl.handled("node.multi.init", e);
            // go forward with no subnets
        }
    }


    @Override
    protected void initDownstream(final @Nullable Bundle savedState) {
        final int n = downstreamStates.length;
        for (int i = 0; i < n; ++i) {
            final DownstreamState state = downstreamStates[i];
            state.downstream.node.init(new MessageControlChannel() {
                @Override
                public void onActive(boolean active) {
                    state.upActive = active;
                    setActiveDownstream();
                }

                @Override
                public void onMessageControl(MessageControl mc) {
                    switch (mc.dir) {
                        case SEND:
                            // route to the active
                            MultiNode.this.onMessageControl(mc);
                            break;
                        case RECEIVE:
                            // up
                            upstream.onMessageControl(mc);
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                }

                @Override
                public MessageControlState getMessageControlState() {
                    return MultiNode.this.getMessageControlState();
                }

                @Override
                public void post(Runnable r) {
                    MultiNode.this.post(r);
                }

                @Override
                public void postDelayed(Runnable r, int delayMs) {
                    MultiNode.this.postDelayed(r, delayMs);
                }

                @Override
                public Scheduler getScheduler() {
                    return MultiNode.this.getScheduler();
                }
            }, savedState);
        }
    }

    @Override
    public void onActive(boolean active) {
        this.active = active;
        setActiveDownstream();
        // TODO on inactive with pending, should pending be sent back upstream?
    }

    @Override
    public void onMessageControl(MessageControl mc) {
        // filter incompatible downstreams
        // adjust the compatibility state to the AND of all seen messages
        // see #3 https://github.com/nextopio/nextop-client/issues/3
        // TODO reset the compatibility state at some point
        if (contains(localSubnets, mc.message.route.via.authority)) {
            // filter incompatible downstreams
            boolean modified = false;
            for (DownstreamState state : downstreamStates) {
                if (state.compatible && !state.downstream.support.contains(Downstream.Support.LOCAL)) {
                    state.compatible = false;
                    modified = true;
                }
            }
            if (modified) {
                setActiveDownstream();
            }
        }

        @Nullable MessageControlNode activeDownstream = getActiveDownstream();
        if (null != activeDownstream) {
            activeDownstream.onMessageControl(mc);
        } else {
            // append to the queue for when the active state flips, see #setActiveDownstream
            pendingMessageControls.add(mc);
        }
    }



    private static final class DownstreamState {
        final Downstream downstream;
        // set by the downstream up into the multi node
        boolean upActive;
        // set by the multi node into the downstream
        boolean downActive = false;

        /** this is set false depending on the downstream support for messages.
         * @see Downstream#support */
        // TODO currently if set, it is never reset; do that at some point
        boolean compatible = true;

        DownstreamState(Downstream downstream, boolean upActive) {
            this.downstream = downstream;
            this.upActive = upActive;
        }
    }


    public static final class Downstream {
        public static enum Support {
            LOCAL
        }


        public static Downstream create(MessageControlNode node, Support ... support) {
            return new Downstream(node, ImmutableSet.copyOf(support));
        }


        public final MessageControlNode node;
        public final ImmutableSet<Support> support;

        Downstream(MessageControlNode node, ImmutableSet<Support> support) {
            this.node = node;
            this.support = support;
        }
    }



    /////// SUBNET ///////
    /* this is used to address #3
     * @see https://github.com/nextopio/nextop-client/issues/3 */

    private static Collection<Subnet> findLocalSubnets() throws IOException {
        Queue<NetworkInterface> is = new LinkedList<NetworkInterface>();
        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            is.add(e.nextElement());
        }

        List<Subnet> subnets = new ArrayList<Subnet>(4);
        for (NetworkInterface i; null != (i = is.poll()); ) {
            for (InterfaceAddress ia : i.getInterfaceAddresses()) {
                subnets.add(Subnet.valueOf(ia));
            }
            @Nullable NetworkInterface p = i.getParent();
            if (null != p) {
                is.offer(p);
            }
        }
        return subnets;
    }

    private static final class Subnet {
        static Subnet valueOf(InterfaceAddress ia) {
            InetAddress a = ia.getAddress();
            return new Subnet(a.getHostName(), bits(a.getAddress()), ia.getNetworkPrefixLength());
        }


        final String host;
        final BitSet address;
        final int prefixLength;

        Subnet(String host, BitSet address, int prefixLength) {
            this.host = host;
            this.address = address;
            this.prefixLength = prefixLength;
        }
    }
    private static BitSet bits(byte[] bytes) {
        // TODO java7: BitSet.valueOf(bytes)
        BitSet bits = new BitSet(8 * bytes.length);
        for (int i = 0, n = bytes.length; i < n; ++i) {
            int b = 0xFF & bytes[i];
            int j = 8 * i;
            for (int k = 0; k < 8; ++k) {
                bits.set(j + k, 0 != (b >>> (7 - k)));
            }
        }
        return bits;
    }

    static boolean contains(Collection<Subnet> subnets, Authority authority) {
        switch (authority.type) {
            case LOCAL:
                return false;
            case NAMED:
                for (Subnet subnet : subnets) {
                    if (subnet.host.equals(authority.getHost())) {
                        return true;
                    }
                }
                return false;
            case IP:
                BitSet address = bits(authority.getIp().getAddress());
                top:
                for (Subnet subnet : subnets) {
                    for (int i = 0, n = subnet.prefixLength; i < n; ++i) {
                        if (address.get(i) != subnet.address.get(i)) {
                            continue top;
                        }
                    }
                    return true;
                }
                return false;
            default:
                throw new IllegalArgumentException();
        }
    }

}
