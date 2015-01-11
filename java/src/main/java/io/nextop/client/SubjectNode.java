package io.nextop.client;

import io.nextop.Message;

// FIXME top level send, receive, mirror
// FIXME rename message controller?
// FIXME start with send and simulating failovers
// FIXME call this a SubjectNode
public class SubjectNode extends MessageControlNode {

    public SubjectNode(MessageControlNode downstream) {

    }


    void send(Message message);

}
