package io.nextop.client;

// reactor pattern where messages in get routed the active controller,
// and surface messages out in both directions
// two channels: down and up
public interface MessageControlChannel extends MessageContext {

    void onActive(boolean active);
    void onMessageControl(MessageControl mc);

    /** thread-safe */
    MessageControlState getMessageControlState();
}
