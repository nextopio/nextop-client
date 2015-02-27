package io.nextop.client.node;

import io.nextop.Message;
import io.nextop.client.MessageControlNode;

// this node maintains multiple (n) downstream nodes ranked by preference
// routes control to the highest pref downsteam that is active
// when the active state of a downstream changes, this node ensures that the active state of the highest downstream is exclusively set
public class MultiNode extends AbstractMessageControlNode {

    private final MessageControlNode[] downstreams;


    public MultiNode(MessageControlNode ... downstreams) {
        this.downstreams = downstreams;
    }




}
