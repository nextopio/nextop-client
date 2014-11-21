package io.nextop.client.android;


import android.support.annotation.Nullable;

import java.io.IOException;

// TODO has a transport in it
// TODO however, tests want to set a transport that is a f
public interface ConnectionContext {
    Connection get();
    Connection get(Message m);

    void close();



    void setTransportFactory(@Nullable TransportFactory tf);


    interface TransportFactory {
        Transport create(Link link, String host, int port) throws IOException;
    }


    // TODO these events should probably be on the Transport too



    // SYSTEM EVENTS

    void onConnectivityEvent(ConnectivityEvent e);
//    void onConditionEvent(ConditionEvent e);


    public static enum Link {
        WIFI,
        CELL
    }

    public static final class ConnectivityEvent {
        Link link;
        boolean connected;
    }

//    public static final class ConditionEvent {
//        Link link;
//        float downBytesPerSecond;
//        float downPacketLoss;
//        float upBytesPerSecond;
//        float upPacketLoss;
//    }

}
