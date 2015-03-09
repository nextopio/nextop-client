package io.nextop.client.node;

import io.nextop.client.MessageControl;
import io.nextop.client.MessageControlChannel;
import io.nextop.client.MessageControlNode;
import io.nextop.client.MessageControlState;
import rx.Scheduler;

import javax.annotation.Nullable;

public class PassthroughNode extends AbstractMessageControlNode {

    MessageControlNode downstream;

    public PassthroughNode(MessageControlNode downstream) {
        this.downstream = downstream;
    }


    @Override
    protected void initDownstream(final @Nullable Bundle savedState) {
        downstream.init(new MessageControlChannel() {
            @Override
            public void onActive(boolean active) {
                upstream.onActive(active);
            }

            @Override
            public void onMessageControl(MessageControl mc) {
                upstream.onMessageControl(mc);
            }

            @Override
            public MessageControlState getMessageControlState() {
                return upstream.getMessageControlState();
            }

            @Override
            public void post(Runnable r) {
                upstream.post(r);
            }

            @Override
            public void postDelayed(Runnable r, int delayMs) {
                upstream.postDelayed(r, delayMs);
            }

            @Override
            public Scheduler getScheduler() {
                return upstream.getScheduler();
            }
        }, savedState);
    }

    @Override
    public void onActive(boolean active) {
        downstream.onActive(active);
    }

    @Override
    public void onMessageControl(MessageControl mc) {
        downstream.onMessageControl(mc);
    }
}
