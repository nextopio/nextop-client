package io.nextop.client;

import java.io.IOException;

public class MultiTransport implements Transport {


    // connects to each Transport

    // can make one transport unavailable and transfer messages to another transport
    // middle man to handle nack issues (out of order issues)
    // where a transport committed a message but is still waiting on the reply

}

// NextopTransport
// HttpTransport



interface MessageControlListener {
    void onAvailableChanged();
    void onMessageControl(MessageControl mc);
}

class MessageControl {
    static enum Type {
        SEND_ACK, // follow up to send_nack (?)
        SEND_ERROR, // follow up to send_nack (?)
        SEND_NACK, // represents the case where the transport has committed the message to the remote but not received a response
        SEND,
        RECEIVE,
        RECEIVE_ACK
        // TODO receive nack?
    }


    Message message;

}

class Message {
    static enum Type {
        CONTROL,
        DATA
    }

    int priority;

    // target is a method+path
//    Target target;
    // via is a scheme+authority
//    Via via;
}


interface Transport extends MessageControlListener {
    void start();
    void stop();

    void addMessageControlListener(MessageControlListener ml);

    void setWireFactory(WireFactory wf);

    // when receive available=false, shut down stream
    // and send all unack'd messages back up onMessageControl
    // vice-versa, when receive available=true, expect to start
    // sending messages onMessageControl
}

interface WireFactory {
    Wire create(Authority authority);
}

interface Wire extends AutoCloseable {
    void init() throws IOException;
    void send(byte[] buffer, int offset, int n) throws IOException;
    void read(byte[] buffer, int offset, int n) throws IOException;
}
