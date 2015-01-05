package io.nextop.client.android;


import android.net.Uri;
import android.support.annotation.Nullable;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import java.io.IOException;


// TODO config path: https://$accesskey.nextop.io/config
// TODO GET on the path returns the entire config as JSON (all possible keys are represented with their current value)
// TODO POST on the path a JSON object. These keys overwrite the existing values in the config
public interface NxClient {
    /** Amortized number of bytes that will fit into a single outgoing packet
     * (control overhead takes the rest). The client may not always fit this many bytes
     * into a packet, but on average it will. */
    int OUT_PACKET_MESSAGE_BYTES = 1300;


    // AUTH

    /* the access key and grant keys are never forwarded beyond the Nextop proxy.
     * this would be a security leak. */

    /* the access key is the domain for all Nextop messages.
     * Messages for the key have URI  https://$accesskey.nextop.io/$id . */
    void auth(@Nullable String accessKey);

    /* permissions in Nextop are done per key. A key is analogous to a Unix group.
     * Each client can have zero or more keys, granting it the
     * union of the permissions associated with the keys.
     * Usually an app will have at least three keys: "client", "server", and "admin". */
    void grantKey(String key);
    void revokeKey(String key);


    // SCHEDULING

    Scheduler getScheduler();
    NxClient on(Scheduler s);



    // MESSAGING


    // receive is always load balanced
    // the same path is sent to the same client, for consistency on same objects
    // (see common path routing notes)
    Receiver<NxMessage> receiver(NxUri p);
    // mirror is a copy of each message on path
    // mirror can get just the headers (headOnly()) or the full message (default)
    // FIXME
//    Mirror<NxMessage> mirror(NxUri p);

    // @see NxSession#sender
    Sender<NxMessage> sender(NxUri p);

    // hold until ack; if not nack, auto-acks at the end of dispatching the message
    // @see NxSession#nack
    void nack(NxUri id);
    // only needed if prior #nack called
    // @see NxSession#ack
    void ack(NxUri id);

    // halts sending of the message (at whatever stage is possible; may not be possible)
    // signal the communication on this bind is complete. subscribers to the bind will be given an error
    // @see NxSession#cancel
    void cancel(NxUri id);
    // signal the communication on this bind is complete. subscribers to the bind will be completed.
    // @see NxSession#complete
    void complete(NxUri id);


    /** delay outgoing control messages (e.g. ack, cancel, subscribe, unsubscribe, complete) to group them
     * or attach them to an outgoing message. */
    void setControlDelay(boolean controlDelay);


    // CACHES

    /* Each client has two LRU caches:
     * - disk network cache, for all cacheable messages.
     * - memory image cache, for decoded images.
     * (based on Volley, which has the best network performance on Android) */

    CacheController getNetworkCache();
    CacheController getImageCache();



    // METRIC TAGS

    // attach to all unsent messages and future messages

    void addTag(String name, String value);
    void removeTag(String name);



    // INTROSPECTION

    Observable<MessageStatus> listMessages();
    Observable<MessageStatus> messageStatus(NxUri id);
//    Observable<Message> get(NxUri id);

    Observable<NxMetrics> metrics(NxUri p);

    // a bind is when a message ID is connected to a path
    // the bind is open/subscribed as long at both sides are connected.
    // each side can time out or have a subscription->0 transition.
    // in those cases, the bind is closed
    // FIXME Bind should probably be session. figure that out
    Observable<Bind> listBinds(NxUri p);
    Observable<ReceivePath> listReceivePaths();



    // TODO admin. create keys, set permissions per key









    // SYSTEM EVENTS

    void onConnectivityEvent(ConnectivityEvent e);
//    void onConditionEvent(ConditionEvent e);


    // TRANSPORT

    void setTransportFactory(@Nullable TransportFactory tf);


    /** ordering guarantee: messages of the same priority from the same sender (possible on different threads),
     * or from different senders on the same thread, will be sent in order.
     *
     * (thread safe) */
    final class Sender<M extends NxMessage> {
        public Receiver send(M m) {
            // FIXME
            return null;
        }

        public Receiver send() {
            // FIXME
            return null;
        }




        public Sender<NxImageMessage> encodeImages(EncoderConfig c) {
            // FIXME
            return null;
        }


        /** enable delay for this message (similar to TCP delay/Nagle's).
         * In terms of order, delayed messages are a lower priority
         * than non-delayed messages of the same priority.
         */
        public Sender<M> setDelay(boolean delay) {
            // FIXME
            return null;
        }


        // also does some rotation, etc, for front facing camera images
        public static final class EncoderConfig {
            int maxWidth;
            int maxHeight;
            float quality;
        }


        /////// ANDROID ///////


        public Receiver send(NxByteString body) {

        }
        public Receiver send(byte[] body) {

        }
        public Receiver send(String body) {

        }
        public Receiver send(int body) {

        }
        public Receiver send(long body) {

        }
        public Receiver send(float body) {

        }
        public Receiver send(double body) {

        }
    }


    final class Receiver<M extends NxMessage> extends Observable<NxSession<M>> {
        final NxUri id;


        Receiver(NxUri id) {
            super(new OnSubscribe<M>() {
                @Override
                public void call(Subscriber<? super M> subscriber) {

                }
            });
            this.id = id;
        }

        // FIXME I'm not sure this is very useful. punt for initial API
        // receive all messages for this receiver, then complete
//        public Receiver<M> drain() {
//
//        }

        public Receiver<NxImageMessage> decodeImages(DecoderConfig c) {
            // FIXME
            return null;
        }


        public static final class DecoderConfig {
            int maxWidth;
            int maxHeight;
        }
    }



    interface CacheController {
        void setSize(int bytes);
        void trim(float f);
    }




    final class MessageStatus {
        static enum CompletionStatus {
            ACKED,
            CANCELED,
            PENDING
        }

        public final NxUri id;
        public final CompletionStatus completionStatus;

        // TODO id, path, size, transfer stats

        MessageStatus(NxUri id, CompletionStatus completionStatus) {
            this.id = id;
            this.completionStatus = completionStatus;
        }
    }

    final class Bind {
        public final NxUri id;
        public final boolean open;

        Bind(NxUri id, boolean open) {
            this.id = id;
            this.open = open;
        }
    }

    final class ReceivePath {
        public final NxUri p;
        public final boolean open;

        ReceivePath(NxUri p, boolean open) {
            this.p = p;
            this.open = open;
        }
    }




    enum Link {
        WIFI,
        CELL
    }

    final class ConnectivityEvent {
        public final Link link;
        public final boolean connected;

        ConnectivityEvent(Link link, boolean connected) {
            this.link = link;
            this.connected = connected;
        }
    }



    interface TransportFactory {
        Transport create(Link link, String host, int port) throws IOException;
    }



    interface Transport {
        void send(byte[] frame) throws IOException;
        rx.Observable<byte[]> receive();

        void open() throws IOException;
        void close() throws IOException;
    }


}
