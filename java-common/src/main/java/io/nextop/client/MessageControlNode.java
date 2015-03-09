package io.nextop.client;

import java.util.List;

// all message control nodes run on a single thread (handler) controlled by the top level node,
// via post and postDelayed
public interface MessageControlNode extends MessageControlChannel {


    void init(MessageControlChannel upstream);
    
    void start();
    void stop();







    // notes

    // when receive available=false, shut down stream
    // and send all unack'd messages back up onMessageControl
    // vice-versa, when receive available=true, expect to start
    // sending messages onMessageControl
}
