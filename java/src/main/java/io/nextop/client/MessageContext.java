package io.nextop.client;

public class MessageContext implements MessageControlChannel {

    // onActive, onTransfer, onMessageControl all should error out if called

    // implement thread handler

    private MessageControlState mcs;


    public MessageContext(MessageControlState mcs) {
        this.mcs = mcs;
    }




}
