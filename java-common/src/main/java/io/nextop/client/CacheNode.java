package io.nextop.client;

// FIXME can this be done via DurableNode?
public class CacheNode /* FIXME */ extends PassthroughNode {

    public CacheNode(MessageControlNode downstream) {
        super(downstream);
    }

}
