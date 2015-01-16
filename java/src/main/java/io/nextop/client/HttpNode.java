package io.nextop.client;

import io.nextop.Message;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HttpNode extends AbstractMessageControlNode {

    private Executor executor;
//    private CloseableHttpClient httpClient;


    public HttpNode(Wire.Factory wireFactory) {
        this(wireFactory, Executors.newCachedThreadPool());
    }

    public HttpNode(Wire.Factory wireFactory, Executor executor) {

        this.executor = executor;
//        httpClient = HttpClients.createDefault();
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
                send(mc.message);
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



    private void send(final Message message) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                // FIXME
                // 0. translate the nextop message to a httpclient request
                // 1. make the request
                // 2. translate the result to a reply message and surface it up with upstream.onMessageControl
                // use post() to surface back onto the node
            }
        });
    }
}
