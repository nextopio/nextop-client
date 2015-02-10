package io.nextop.client;

import io.nextop.Message;
import io.nextop.Route;

public final class MessageControl {
    static enum Direction {
        SEND,
        RECEIVE
    }
    static enum Type {

        SUBSCRIBE, // <->
        UNSUBSCRIBE, // <->

        MESSAGE,
        ACK,
        NACK,
        ERROR,
        COMPLETE

//        SEND, // ->
//        SEND_ACK, // <- message has been successfully sent. can delete locally
//        SEND_NACK, // -> cancel
//        SEND_ERROR, // <- message rejected from the other side
//        SEND_COMPLETE,
//
//        RECEIVE, // <- here's a message, hasn't been ack'd yet
//        RECEIVE_ACK, // ->
//        RECEIVE_NACK, // -> error handling response; may send back for another receiver
//        RECEIVE_ERROR, // <- message is gone (ejected/deleted after timeout, etc)
//        RECEIVE_COMPLETE
    }


    public static MessageControl send(Message message) {
        return send(Type.MESSAGE, message);
    }
    public static MessageControl send(Type type, Route route) {
        Message spec = Message.newBuilder().setRoute(route).build();
        return send(type, spec);
    }
    public static MessageControl send(Type type, Message message) {
        return create(Direction.SEND, type, message);
    }

    public static MessageControl receive(Message message) {
        return receive(Type.MESSAGE, message);
    }
    public static MessageControl receive(Type type, Message message) {
        return receive(type, message);
    }
    public static MessageControl receive(Type type, Route route) {
        Message spec = Message.newBuilder().setRoute(route).build();
        return create(Direction.RECEIVE, type, spec);
    }

    public static MessageControl create(Direction dir, Type type, Message message) {
        if (null == dir) {
            throw new IllegalArgumentException();
        }
        if (null == type) {
            throw new IllegalArgumentException();
        }
        if (null == message) {
            throw new IllegalArgumentException();
        }
        return new MessageControl(dir, type, message);
    }




    public final Direction dir;
    public final Type type;
    public final Message message;

    MessageControl(Direction dir, Type type, Message message) {
        this.dir = dir;
        this.type = type;
        this.message = message;
    }

}
