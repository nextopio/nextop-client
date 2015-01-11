package io.nextop.client;

import java.util.List;

public class MessageControlState {
    // FIXME store messages ordered by priority (use a heap)
    List<MessageControl> mcs;


    // TODO append control, this may compress the list (on subscribe->unsubscribe)

}
