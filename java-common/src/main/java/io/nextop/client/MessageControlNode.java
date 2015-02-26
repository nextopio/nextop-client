package io.nextop.client;

import java.util.List;


// unless stated otherwise, all calls to the API must be on the context thread
// all message control nodes run on a single thread (handler) controlled by the top level node,
// via post and postDelayed
public interface MessageControlNode extends MessageControlChannel {


    void init(MessageControlChannel upstream);

    // onActive(active) may be called multiple times
    // typically these are called when all used network links drop out or comes back
    // (via connectivity events, etc)







    // notes

    // when receive available=false, shut down stream
    // and send all unack'd messages back up onMessageControl
    // vice-versa, when receive available=true, expect to start
    // sending messages onMessageControl
}
