package io.nextop.client.node;

import io.nextop.client.MessageControlChannel;
import io.nextop.client.MessageControlNode;
import io.nextop.client.MessageControlState;
import rx.Scheduler;

import javax.annotation.Nullable;

public abstract class AbstractMessageControlNode implements MessageControlNode {

    @Nullable
    protected MessageControlChannel upstream = null;


    public AbstractMessageControlNode() {

    }


    private void checkUpstream() {
        if (null == upstream) {
            throw new IllegalStateException();
        }
    }


    protected void initSelf() {
        // Do nothing
    }
    protected void initDownstream() {
        // Do nothing
    }



    @Override
    public final void init(MessageControlChannel upstream) {
        if (null != this.upstream) {
            throw new IllegalStateException();
        }
        this.upstream = upstream;
        initSelf();
        initDownstream();
    }

    @Override
    public final MessageControlState getMessageControlState() {
        return upstream.getMessageControlState();
    }

    @Override
    public final void post(Runnable r) {
        checkUpstream();
        upstream.post(r);
    }

    @Override
    public final void postDelayed(Runnable r, int delayMs) {
        checkUpstream();
        upstream.postDelayed(r, delayMs);
    }

    @Override
    public final Scheduler getScheduler() {
        checkUpstream();
        return upstream.getScheduler();
    }
}
