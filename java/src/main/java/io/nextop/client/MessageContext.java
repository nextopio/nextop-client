package io.nextop.client;

public class MessageContext implements MessageControlChannel {

    // onActive, onTransfer, onMessageControl all should error out if called

    // implement thread handler

    private MessageControlState mcs;


    public MessageContext(MessageControlState mcs) {
        this.mcs = mcs;
    }


    @Override
    public void onActive(boolean active, MessageControlMetrics metrics) {

    }

    @Override
    public void onTransfer(MessageControlState mcs) {

    }

    @Override
    public void onMessageControl(MessageControl mc) {

    }

    @Override
    public void post(Runnable r) {

    }

    @Override
    public void postDelayed(Runnable r, int delayMs) {

    }
}
