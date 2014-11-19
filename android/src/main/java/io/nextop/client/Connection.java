package io.nextop.client;

import android.net.Uri;
import android.support.annotation.Nullable;
import rx.Observable;

public interface Connection {
    Message send(Path p, Message m, @Nullable SendOptions opts);
    Observable<Message> receive(Path p, @Nullable ReceiveOptions opts);
    void ack(Uri id, @Nullable AckOptions opts);

    Observable<Uri> listMessages();
    Observable<MessageStatus> status(Uri id);

    void close();


    public class SendOptions {

    }
    public class ReceiveOptions {

    }
    public class AckOptions {

    }

    public final class MessageStatus {

    }


    public final class Builder {
        private final Connection c;
        private final @Nullable Message cm;


        public Builder(Connection c, @Nullable Message cm) {
            this.c = c;
            this.cm = cm;
        }

        public Builder send(Uri p, Message m, @Nullable SendOptions opts) {

        }
        public Builder send(Message m, @Nullable SendOptions opts) {

        }
        Observable<Builder> receive(Uri p, @Nullable ReceiveOptions opts) {

        }
        Observable<Builder> receive(@Nullable ReceiveOptions opts) {

        }
        void ack(@Nullable AckOptions opts) {

        }
    }
}
