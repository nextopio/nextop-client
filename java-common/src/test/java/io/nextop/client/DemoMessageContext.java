package io.nextop.client;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


// single threaded
public class DemoMessageContext implements MessageControlChannel {

    private final MessageControlState mcs;
    private final ScheduledExecutorService executor;


    public DemoMessageContext(MessageControlState mcs) {
        this.mcs = mcs;
        executor = Executors.newSingleThreadScheduledExecutor();
    }


    @Override
    public void post(Runnable r) {
        executor.execute(r);
    }

    @Override
    public void postDelayed(Runnable r, int delayMs) {
        executor.schedule(r, delayMs, TimeUnit.MILLISECONDS);
    }


    @Override
    public void onActive(boolean active, MessageControlMetrics metrics) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onTransfer(MessageControlState mcs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMessageControl(MessageControl mc) {
        throw new UnsupportedOperationException();
    }
}
