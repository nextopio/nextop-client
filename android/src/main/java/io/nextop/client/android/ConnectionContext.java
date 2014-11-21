package io.nextop.client.android;


import android.net.Uri;
import android.support.annotation.Nullable;
import rx.Observable;

import java.io.IOException;

public interface ConnectionContext {
    Observable<Message> send(Uri p, Message m);
    // receive is always load balanced
    // the same path is sent to the same client, for consistency on same objects
    Observable<Message> receive(Uri p);
    // always auto ack when observer completes
//    void ack(Uri id);
    // seems like a rare case
//    void cancel(Uri id);


    // the control host is "publickeyfingerprint.nextop.io", which is the namespace for message ids
    // e.g. publickeyfingerprint.nextop.io/ID
    String getControlHost();



    // INTROSPECTION

    Observable<MessageStatus> listMessages();
    Observable<MessageStatus> messageStatus(Uri id);
//    Observable<Message> get(Uri id);

    Observable<Metrics> metrics(Uri p);

    // a bind is when a message ID is connected to a path
    // the bind is open/subscribed as long at both sides are connected.
    // each side can time out or have a subscription->0 transition.
    // in those cases, the bind is closed
    Observable<Bind> listBinds(Uri p);
    Observable<ReceivePath> listReceivePaths();



    public final class MessageStatus {
        static enum CompletionStatus {
            ACKED,
            CANCELED,
            PENDING
        }

        final Uri id;
        final CompletionStatus completionStatus;

        // TODO id, path, size, transfer stats
    }

    public static final class Bind {
        final Uri id;
        final boolean open;
    }

    public static final class ReceivePath {
        final String path;
        final boolean open;
    }


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
