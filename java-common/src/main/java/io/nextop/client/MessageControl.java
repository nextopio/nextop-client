package io.nextop.client;

import io.nextop.Message;
import io.nextop.Route;
import io.nextop.WireValue;

import java.util.HashMap;
import java.util.Map;

public final class MessageControl {
    public static enum Direction {
        SEND,
        RECEIVE
    }
    public static enum Type {
        // FIXME
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


    /////// SERIALIZATION ///////

    private static final int S_VERSION = 1;

    private static final String S_KEY_VERSION = "version";
    private static final String S_KEY_DIR = "dir";
    private static final String S_KEY_TYPE = "type";
    private static final String S_KEY_MESSAGE = "message";


    public static WireValue toWireValue(MessageControl mc) {
        Map<Object, Object> map = new HashMap<Object, Object>(32);
        map.put(S_KEY_VERSION, S_VERSION);
        // v1
        map.put(S_KEY_DIR, mc.dir.toString());
        map.put(S_KEY_TYPE, mc.type.toString());
        map.put(S_KEY_MESSAGE, mc.message);
        return WireValue.of(map);
    }

    public static MessageControl fromWireValue(WireValue value) {
        Map<WireValue, WireValue> map = value.asMap();
        int version = map.get(S_KEY_VERSION).asInt();
        switch (version) {
            default:
                // from the future
                // attempt to parse it as the last known version
                // (if fails, the parsing will error out)
                if (version < S_VERSION) {
                    throw new IllegalArgumentException();
                } // else fall through
            case 1:
                Direction dir = Direction.valueOf(map.get(S_KEY_DIR).asString());
                Type type = Type.valueOf(map.get(S_KEY_TYPE).asString());
                Message message = map.get(S_KEY_MESSAGE).asMessage();
                return new MessageControl(dir, type, message);
        }
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

    @Override
    public int hashCode() {
        int c = dir.hashCode();
        c = 31 * c + type.hashCode();
        c = 31 * c + message.hashCode();
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MessageControl)) {
            return false;
        }

        MessageControl b = (MessageControl) o;
        return dir.equals(b.dir)
                && type.equals(b.type)
                && message.equals(b.message);
    }
}
