package io.nextop.client;

import io.nextop.Message;

// FIXME top level send, receive, mirror
// FIXME rename message controller?
// FIXME start with send and simulating failovers
// FIXME call this a SubjectNode
public class SubjectNode extends AbstractMessageControlNode {

    public SubjectNode(MessageControlNode downstream) {

    }


    public void send(Message message) {
        // FIXME
    }

    @Override
    public void onActive(boolean active, MessageControlMetrics metrics) {

    }

    @Override
    public void onTransfer(MessageControlState mcs) {

    }

    @Override
    public void onMessageControl(MessageControl mc) {

    }
}
