package io.nextop.client;

import io.nextop.Message;
import io.nextop.Route;
import io.nextop.client.retry.SendStrategy;
import io.nextop.org.apache.http.HttpResponse;
import io.nextop.org.apache.http.client.HttpClient;
import io.nextop.org.apache.http.client.methods.HttpUriRequest;
import io.nextop.org.apache.http.impl.client.DefaultHttpClient;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// FIXME ordering and retry
// FIXME use a package-shifted version of HttpClient
public class HttpNode extends AbstractMessageControlNode {

    private Executor executor;
    private HttpClient httpClient;

    private SendStrategy sendStrategy = SendStrategy.INDEFINITE;


    // FIXME 0.1.1
//    Map<Route, TransferProgress> progresses;



    public HttpNode() {
        // FIXME 0.2 ordering and expand to multiple threads
        this(Executors.newSingleThreadExecutor());
    }

    public HttpNode(Executor executor) {

        this.executor = executor;
        // FIXME 0.1.1 jarjar httpclient
        httpClient = new DefaultHttpClient();
                //HttpClients.createDefault();
    }


    @Override
    public void onActive(boolean active, MessageControlMetrics metrics) {
        // FIXME on false, upstream.onTransfer(mcs)
    }

    @Override
    public void onTransfer(MessageControlState mcs) {
        // FIXME
    }


    @Override
    public void onMessageControl(MessageControl mc) {
        switch (mc.type) {
            case SEND:
                // FIXME update the mcs?
                onSend(mc.message);
                break;

            case SUBSCRIBE:
                // FIXME put this in the mcs
                break;

            case UNSUBSCRIBE:
                // FIXME put this in the mcs
                break;

            case SEND_NACK:
                // FIXME update the mcs
                break;

            case RECEIVE_ACK:
                // FIXME update the mcs
                break;

            case RECEIVE_NACK:
                // FIXME update the mcs
                break;
        }
    }



    private void onSend(Message message) {
        if (message.route.via.isLocal()) {
            // FIXME 0.1.1 a control message
            // leave it hanging for now
        } else {
            executor.execute(new RequestWorker(message));
        }
    }

    private void onSendError(Message message) {
        upstream.onMessageControl(new MessageControl(MessageControl.Type.SEND_ERROR, message));
    }


    private void onReceive(Message message) {
        upstream.onMessageControl(new MessageControl(MessageControl.Type.RECEIVE, message));
        upstream.onMessageControl(new MessageControl(MessageControl.Type.RECEIVE_COMPLETE, message.toSpec()));
    }

    private void onReceiveError(Message message) {
        upstream.onMessageControl(new MessageControl(MessageControl.Type.RECEIVE_ERROR, message));
    }



    private final class RequestWorker implements Runnable {
        final Message requestMessage;

        RequestWorker(Message requestMessage) {
            this.requestMessage = requestMessage;
        }

        @Override
        public void run() {
            final HttpUriRequest request;
            try {
                request = Message.toHttpRequest(requestMessage);
            } catch (Exception e) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        onSendError(requestMessage);
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
                        onSendError(requestMessage);
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
                        onReceiveError(Message.newBuilder().setRoute(requestMessage.inboxRoute()).build());
                    }
                });
                return;
            }

            post(new Runnable() {
                @Override
                public void run() {
                    onReceive(responseMessage);
                }
            });
        }
    }



    private static final class TransferProgressState {
        final Route route;
        float progress;

        TransferProgressState(Route route, float progress) {
            this.route = route;
            this.progress = progress;
        }

    }

}
