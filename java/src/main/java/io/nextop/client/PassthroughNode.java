package io.nextop.client;

public class PassthroughNode extends AbstractMessageControlNode {

    MessageControlNode downstream;

    public PassthroughNode(MessageControlNode downstream) {
        this.downstream = downstream;
    }


    @Override
    protected void initDownstream() {
        downstream.init(new MessageControlChannel() {
            @Override
            public void onActive(boolean active, MessageControlMetrics metrics) {
                upstream.onActive(active, metrics);
            }

            @Override
            public void onTransfer(MessageControlState mcs) {
                upstream.onTransfer(mcs);
            }

            @Override
            public void onMessageControl(MessageControl mc) {
                upstream.onMessageControl(mc);
            }

            @Override
            public void post(Runnable r) {
                upstream.post(r);
            }

            @Override
            public void postDelayed(Runnable r, int delayMs) {
                upstream.postDelayed(r, delayMs);
            }
        });
    }

    @Override
    protected void startDownstream() {
        downstream.start();
    }

    @Override
    protected void stopDownstream() {
        downstream.stop();
    }


    @Override
    public void onActive(boolean active, MessageControlMetrics metrics) {
        downstream.onActive(active, metrics);
    }

    @Override
    public void onTransfer(MessageControlState mcs) {
        downstream.onTransfer(mcs);
    }

    @Override
    public void onMessageControl(MessageControl mc) {
        downstream.onMessageControl(mc);
    }
}
