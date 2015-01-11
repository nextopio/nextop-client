package io.nextop.client;

// saves messages to a sqlite db and restores pending messages on start
// smooths out issues like receiving the same message twice (can check if message already surfaced)
// on start emits all receive messages that have not been ackd
public class DurableNode /* FIXME */ extends PassthroughNode {
    // (datasourceprovider)

    public DurableNode(MessageControlNode downstream) {
        super(downstream);
    }
}
