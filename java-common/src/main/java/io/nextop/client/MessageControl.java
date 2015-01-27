package io.nextop.client;

import io.nextop.Message;

public final class MessageControl {
    static enum Type {

        SUBSCRIBE, // <->
        UNSUBSCRIBE, // <->

        SEND, // ->
        SEND_ACK, // <- message has been successfully sent. can delete locally
        SEND_NACK, // -> cancel
        SEND_ERROR, // <- message rejected from the other side

        RECEIVE, // <- here's a message, hasn't been ack'd yet
        RECEIVE_ACK, // ->
        RECEIVE_NACK, // -> error handling response; may send back for another receiver
        RECEIVE_ERROR // <- message is gone (ejected/deleted after timeout, etc)
    }

    public final Type type;
    public final Message message;
    // TODO direction: out, in

    public MessageControl(Type type, Message message) {
        this.type = type;
        this.message = message;
    }

}
