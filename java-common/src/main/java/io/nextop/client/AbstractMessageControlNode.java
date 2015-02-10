package io.nextop.client;

import rx.Scheduler;

import javax.annotation.Nullable;

public abstract class AbstractMessageControlNode implements MessageControlNode {

    @Nullable
    protected MessageControlChannel upstream = null;

    @Nullable
    protected MessageControlState mcs = null;



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
    protected void startSelf() {
        // Do nothing
    }
    protected void startDownstream() {
        // Do nothing
    }
    protected void stopSelf() {
        // Do nothing
    }
    protected void stopDownstream() {
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
    public final void start() {
        checkUpstream();
        startDownstream();
        startSelf();
    }

    @Override
    public final void stop() {
        checkUpstream();
        stopDownstream();
        stopSelf();
    }


    @Override
    public void onTransfer(MessageControlState mcs) {
        this.mcs = mcs;
    }

    @Override
    public void post(Runnable r) {
        checkUpstream();
        upstream.post(r);
    }

    @Override
    public void postDelayed(Runnable r, int delayMs) {
        checkUpstream();
        upstream.postDelayed(r, delayMs);
    }

    @Override
    public Scheduler getScheduler() {
        checkUpstream();
        return upstream.getScheduler();
    }
}
