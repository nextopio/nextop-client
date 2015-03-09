package io.nextop.client;

// proactor (async) pattern where messages in get routed the active controller,
// and surface messages out in both directions
public interface MessageControlChannel extends MessageContext {
    // rules
    // 1. messages routed to the active node
    // 2. after onActive, onTransfer is always called (either from upstream to self if active-true, or from self to upstream is active=false) with the complete message control state, transferring control of the state
    // 3. the active node maintains the message control state until it transfers it
    // 4. messages must pass through upstream first before reaching downstream active
    // 5. messages can only be passed up after subscribe


    // does not forbid unintuitive behavior that does not break the above rules
    // e.g.
    // after a channel calls onTransfer to upstream, it may still call onMessageControl to upstream with lingering messages
    //


    // FIXME kill metrics
    // FIXME kill onTransfer - pass the state in Node init

    // quality is set to 0 into downstream; only the upstream cares about quality
    void onActive(boolean active, MessageControlMetrics metrics);
    void onTransfer(MessageControlState mcs);
    void onMessageControl(MessageControl mc);


}
