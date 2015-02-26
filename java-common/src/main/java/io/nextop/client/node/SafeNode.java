package io.nextop.client.node;

import io.nextop.client.MessageControl;
import io.nextop.client.MessageControlChannel;
import io.nextop.client.MessageControlNode;
import io.nextop.client.MessageControlState;
import rx.Scheduler;

import javax.annotation.Nullable;

/** thread-safe */
public class SafeNode implements MessageControlNode {
    private final MessageControlNode downstream;


    public SafeNode(MessageControlNode downstream) {
        this.downstream = downstream;
    }


    /////// MessageControlNode IMPLEMENTATION ///////

    @Override
    public void init(final @Nullable MessageControlChannel upstream) {
        downstream.post(new Runnable() {
            @Override
            public void run() {
                downstream.init(upstream);
            }
        });
    }


    /////// MessageControlChannel IMPLEMENTATION ///////

    public void onActive(final boolean active) {
        downstream.post(new Runnable() {
            @Override
            public void run() {
                downstream.onActive(active);
            }
        });
    }

    public void onMessageControl(final MessageControl mc) {
        downstream.post(new Runnable() {
            @Override
            public void run() {
                downstream.onMessageControl(mc);
            }
        });
    }

    @Override
    public MessageControlState getMessageControlState() {
        return downstream.getMessageControlState();
    }


    /////// MessageContext IMPLEMENTATION ///////

    public void post(Runnable r) {
        downstream.post(r);
    }

    public void postDelayed(Runnable r, int delayMs) {
        downstream.postDelayed(r, delayMs);
    }

    public Scheduler getScheduler() {
        return downstream.getScheduler();
    }
}
