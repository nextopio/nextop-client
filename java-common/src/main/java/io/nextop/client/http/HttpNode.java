package io.nextop.client.http;

import io.nextop.Id;
import io.nextop.Message;
import io.nextop.Route;
import io.nextop.client.AbstractMessageControlNode;
import io.nextop.client.MessageControl;
import io.nextop.client.MessageControlMetrics;
import io.nextop.client.MessageControlState;
import io.nextop.client.retry.SendStrategy;
import io.nextop.org.apache.http.HttpResponse;
import io.nextop.org.apache.http.client.HttpClient;
import io.nextop.org.apache.http.client.methods.HttpUriRequest;
import io.nextop.org.apache.http.impl.client.DefaultHttpClient;

import javax.annotation.Nullable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class HttpNode extends AbstractMessageControlNode {

    private final HttpClient httpClient;

    // FIXME
    private SendStrategy sendStrategy = SendStrategy.INDEFINITE;

    volatile boolean active = true;



    public HttpNode() {
        httpClient = new DefaultHttpClient();
                //HttpClients.createDefault();
    }


    @Override
    public void onActive(boolean active, MessageControlMetrics metrics) {
        this.active = active;

        if (!active) {
            // FIXME on false, upstream.onTransfer(mcs)
        }
    }

    @Override
    public void onTransfer(MessageControlState mcs) {
        super.onTransfer(mcs);

        if (active) {
            // FIXME can have multiple loopers here because mcs does the correct ordering
            // FIXME parameterize
//            for (int i = 0; i < 8; ++i) {
                new Thread(new RequestLooper()).start();
//            }
        }
    }

    @Override
    public void onMessageControl(MessageControl mc) {
        assert MessageControl.Direction.SEND.equals(mc.dir);
        if (active && !mcs.onActiveMessageControl(mc, upstream)) {
            switch (mc.type) {
                case MESSAGE:
                    mcs.add(mc.message);
                    break;
                default:
                    // ignore
                    break;
            }
        }
    }





    private final class RequestLooper implements Runnable {

        @Override
        public void run() {
            while (active) {
                try {
                    @Nullable MessageControlState.Entry entry = mcs.takeFirstAvailable(HttpNode.this,
                            Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
                    if (null != entry) {
                        try {
                            execute(entry.message, entry);
                        } finally {
                            // FIXME ERROR, COMPLETED
                            mcs.remove(entry.id, MessageControlState.End.COMPLETED);
                        }
                    }
                } catch (InterruptedException e) {
                    // continue
                }
            }

        }

        private void execute(final Message requestMessage, MessageControlState.Entry entry) {
            // FIXME do progress updates



            final HttpUriRequest request;
            try {
                request = Message.toHttpRequest(requestMessage);
            } catch (Exception e) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        upstream.onMessageControl(MessageControl.receive(MessageControl.Type.ERROR, requestMessage));
                    }
                });
                return;
            }

            // FIXME 0.1.1
            // FIXME   surface progress
            // FIXME   retry on transfer up and down if idempotent (resend)
            // FIXME   use jarjar on latest httpclient to have a stable version to work with on android

            // FIXME can do retry here while active {  use supplied retry strategy
            final HttpResponse response;
            try {
                response = httpClient.execute(request);
            } catch (Exception e) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        upstream.onMessageControl(MessageControl.receive(MessageControl.Type.ERROR, requestMessage));
                    }
                });
                return;
            }
            // FIXME }


            // FIXME can't do retry here if not idempotent
            // FIXME *can do* retry here on idempotent (GET, HEAD)
            final Message responseMessage;
            try {
                responseMessage = Message.fromHttpResponse(response).setRoute(requestMessage.inboxRoute()).build();
            } catch (Exception e) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        upstream.onMessageControl(MessageControl.receive(MessageControl.Type.ERROR, requestMessage.inboxRoute()));
                    }
                });
                return;
            }

            // FIXME parse codes
            post(new Runnable() {
                @Override
                public void run() {
                    upstream.onMessageControl(MessageControl.receive(responseMessage));
                    upstream.onMessageControl(MessageControl.receive(MessageControl.Type.COMPLETE, responseMessage.route));
                }
            });
        }

    }




}
