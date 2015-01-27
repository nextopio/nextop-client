package io.nextop;

import android.os.Handler;
import android.os.Looper;
import io.nextop.client.MessageControl;
import io.nextop.client.MessageControlChannel;
import io.nextop.client.MessageControlMetrics;
import io.nextop.client.MessageControlState;

public class AndroidMessageContext implements MessageControlChannel {

    private final MessageControlState mcs;
    private final Handler handler;


    public AndroidMessageContext(MessageControlState mcs) {
        this.mcs = mcs;
        // FIXME use a different looper here
        handler = new Handler(Looper.getMainLooper());
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

