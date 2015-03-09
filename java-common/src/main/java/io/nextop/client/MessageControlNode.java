package io.nextop.client;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// unless stated otherwise, all calls to the API must be on the context thread
// all message control nodes run on a single thread (handler) controlled by the top level node,
// via post and postDelayed
public interface MessageControlNode extends MessageControlChannel {

    // lifecycle:
    // - init
    // - onSaveState+ (snapshot)


    void init(MessageControlChannel upstream, @Nullable Bundle savedState);

    void onSaveState(Bundle savedState);

    // onActive(active) may be called multiple times
    // typically these are called when all used network links drop out or comes back
    // (via connectivity events, etc)


    final class Bundle extends HashMap<String, Serializable> implements Serializable {

    }





    // notes

    // when receive available=false, shut down stream
    // and send all unack'd messages back up onMessageControl
    // vice-versa, when receive available=true, expect to start
    // sending messages onMessageControl
}
