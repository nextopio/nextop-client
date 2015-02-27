package io.nextop.client.node;

import io.nextop.client.MessageControl;
import io.nextop.client.MessageControlChannel;
import io.nextop.client.MessageControlNode;
import io.nextop.client.MessageControlState;

public class MultiNode extends AbstractMessageControlNode {

    private final DownstreamState[] downstreamStates;

    MultiNode(MessageControlNode ... downstreams) {
        int n = downstreams.length;
        downstreamStates = new DownstreamState[n];
        for (int i = 0; i < n; ++i) {
            downstreamStates[i] = new DownstreamState(downstreams[i], false);
        }
    }


    @Override
    protected void initSelf() {
        upstream.onActive(true);
    }

    @Override
    protected void initDownstream() {
        final int n = downstreamStates.length;
        for (int i = 0; i < n; ++i) {
            final DownstreamState state = downstreamStates[i];
            state.downstream.init(new MessageControlChannel() {
                @Override
                public void onActive(boolean active) {
                    state.upActive = active;
                    setDownActive();
                }

                @Override
                public void onMessageControl(MessageControl mc) {
                    switch (mc.dir) {
                        case SEND:
                            // route to the active
                            MultiNode.this.onMessageControl(mc);
                            break;
                        case RECEIVE:
                            // up
                            upstream.onMessageControl(mc);
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                }

                @Override
                public MessageControlState getMessageControlState() {
                    return null;
                }

                @Override
                public void post(Runnable r) {

                }

                @Override
                public void postDelayed(Runnable r, int delayMs) {

                }

                @Override
                public Scheduler getScheduler() {
                    return null;
                }
            });
        }
    }




    private static final class DownstreamState {
        final MessageControlNode downstream;
        // set by the downstream up into the multi node
        boolean upActive;
        // set by the multi node into the downstream
        boolean downActive = false;

        DownstreamState(MessageControlNode downstream, boolean upActive) {
            this.downstream = downstream;
            this.upActive = upActive;
        }
    }


}
