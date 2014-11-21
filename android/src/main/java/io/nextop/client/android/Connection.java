package io.nextop.client.android;

import android.net.Uri;
import android.support.annotation.Nullable;
import rx.Observable;

public interface Connection {
    ConnectionContext getContext();

    // the control host is "publickeyfingerprint.nextop.io", which is the namespace for message ids
    // e.g. publickeyfingerprint.nextop.io/ID
    String getControlHost();


    @Nullable Message get();


    // immutable struct style

    Connection send(Uri p, Message m);
    Connection reply(Message m);
    Observable<Connection> receive(Uri p);
    Observable<Connection> replies();
    void ack();
    void cancel();



//    Message send(Path p, Message m);
//    // receive is always load balanced
//    Observable<Message> receive(Path p);
//    void ack(Uri id);
//    void cancel(Uri id);



    // INTROSPECTION

    Observable<MessageStatus> listMessages();
    Observable<MessageStatus> status(Uri id);
//    Observable<Message> get(Uri id);

    Observable<Metrics> metrics(Uri id);
    Observable<Metrics> metrics(Path p);

    // a bind is when a message ID is connected to a path
    // the bind is open/subscribed as long at both sides are connected.
    // each side can time out or have a subscription->0 transition.
    // in those cases, the bind is closed
    Observable<Bind> listBinds(Path p);
    Observable<Path> listNamedPaths();








    public final class MessageStatus {
        static enum CompletionStatus {
            ACKED,
            CANCELED,
            PENDING
        }

        CompletionStatus completionStatus;

        // TODO id, path, size, transfer stats
    }


}
