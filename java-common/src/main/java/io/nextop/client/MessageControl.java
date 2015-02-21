package io.nextop.client;

import io.nextop.Message;
import io.nextop.Route;

public final class MessageControl {
    public static enum Direction {
        SEND,
        RECEIVE
    }
    public static enum Type {

//        SUBSCRIBE, // <->
//        UNSUBSCRIBE, // <->

        MESSAGE,
        ERROR,
        COMPLETE
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
    public static MessageControl receive(Type type, Route route) {
        Message spec = Message.newBuilder().setRoute(route).build();
        return receive(type, spec);
    }
    public static MessageControl receive(Type type, Message message) {
        return create(Direction.RECEIVE, type, message);
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

    @Override
    public String toString() {
        return String.format("%s %s %s", dir, type, message);
    }
}
