package io.nextop.client;

public interface MessageControlChannel {
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


    // quality is set to 0 into downstream; only the upstream cares about quality
    void onActive(boolean active, MessageControlMetrics metrics);
    void onTransfer(MessageControlState mcs);
    void onMessageControl(MessageControl mc);

    // TODO
    // post
    // postDelayed
    void post(Runnable r);
    void postDelayed(Runnable r, int delayMs);

}
