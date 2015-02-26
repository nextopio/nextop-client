package io.nextop.client.node;

import io.nextop.client.*;
import rx.Scheduler;

import javax.annotation.Nullable;

public class Head implements MessageControlNode {

    /** @return thread-safe node to control the entire tree */
    public static MessageControlNode create(MessageContext context, MessageControlState mcs, MessageControlNode downstream) {
        Head head = new Head(context, mcs, downstream);
        // give a thread-safe interface to the caller
        return new SafeNode(head);
    }


    final MessageContext context;
    final MessageControlState mcs;
    final MessageControlNode downstream;


    Head(MessageContext context, MessageControlState mcs, MessageControlNode downstream) {
        this.context = context;
        this.mcs = mcs;
        this.downstream = downstream;
    }


    /////// MessageControlNode IMPLEMENTATION ///////

    @Override
    public void init(@Nullable MessageControlChannel upstream) {
        if (null != upstream) {
            throw new IllegalArgumentException();
        }
        downstream.init(this);
    }


    /////// MessageControlChannel IMPLEMENTATION ///////

    public void onActive(boolean active) {
        downstream.onActive(active);
    }

    public void onMessageControl(MessageControl mc) {
        downstream.onMessageControl(mc);
    }

    @Override
    public MessageControlState getMessageControlState() {
        return mcs;
    }


    /////// MessageContext IMPLEMENTATION ///////

    public void post(Runnable r) {
        context.post(r);
    }

    public void postDelayed(Runnable r, int delayMs) {
        context.postDelayed(r, delayMs);
    }

    public Scheduler getScheduler() {
        return context.getScheduler();
    }

}
