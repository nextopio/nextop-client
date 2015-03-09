package io.nextop;

import android.os.Handler;
import android.os.Looper;
import io.nextop.client.MessageControl;
import io.nextop.client.MessageControlChannel;
import io.nextop.client.MessageControlMetrics;
import io.nextop.client.MessageControlState;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

public class AndroidMessageContext implements MessageControlChannel {

    private final Handler handler;
    private final Scheduler scheduler;


    public AndroidMessageContext() {
        // FIXME use a different looper here
        handler = new Handler(Looper.getMainLooper());

        scheduler = AndroidSchedulers.handlerThread(handler);
    }


    @Override
    public void post(Runnable r) {
        handler.post(r);
    }

    @Override
    public void postDelayed(Runnable r, int delayMs) {
        handler.postDelayed(r, delayMs);
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
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

