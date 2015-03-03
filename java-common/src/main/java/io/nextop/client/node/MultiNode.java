package io.nextop.client.node;


import io.nextop.client.MessageControl;
import io.nextop.client.MessageControlChannel;
import io.nextop.client.MessageControlNode;
import io.nextop.client.MessageControlState;
import rx.Scheduler;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.Queue;

// multi down

// this node maintains multiple (n) downstream nodes ranked by preference
// routes control to the highest pref downsteam that is active
// when the active state of a downstream changes, this node ensures that the active state of the highest downstream is exclusively set
public class MultiNode extends AbstractMessageControlNode {

    private final DownstreamState[] downstreamStates;

    private Queue<MessageControl> pendingMessageControls = new LinkedList<MessageControl>();

    private boolean active = false;


    public MultiNode(MessageControlNode ... downstreams) {
        int n = downstreams.length;
        downstreamStates = new DownstreamState[n];
        for (int i = 0; i < n; ++i) {
            downstreamStates[i] = new DownstreamState(downstreams[i], false);
        }
    }


    @Nullable
    private MessageControlNode getActiveDownstream() {
        for (DownstreamState state : downstreamStates) {
            if (state.downActive) {
                assert state.upActive;
                return state.downstream;
            }
        }
        return null;
    }

    private void setActiveDownstream() {
        // scan the down states for the first with "upActive=true"
        // if there is another set "downActive=true", unset that, then set the new

        if (!active) {
            clearActiveDownstream();
        } else {

            int firstUpActiveIndex = -1;
            for (int i = 0, n = downstreamStates.length; i < n; ++i) {
                DownstreamState state = downstreamStates[i];
                if (state.upActive) {
                    firstUpActiveIndex = i;
                    break;
                }
            }

            if (firstUpActiveIndex < 0) {
                clearActiveDownstream();
            } else if (!downstreamStates[firstUpActiveIndex].downActive) {
                // unset
                clearActiveDownstream();

                // set
                DownstreamState activeDownstreamState = downstreamStates[firstUpActiveIndex];
                activeDownstreamState.downActive = true;
                activeDownstreamState.downstream.onActive(true);
                for (@Nullable MessageControl mc; null != (mc = pendingMessageControls.poll()); ) {
                    activeDownstreamState.downstream.onMessageControl(mc);
                }
            }
        }
    }

    private void clearActiveDownstream() {
        for (int i = 0, n = downstreamStates.length; i < n; ++i) {
            DownstreamState state = downstreamStates[i];
            if (state.downActive) {
                state.downstream.onActive(false);
                break;
            }
        }
        assert null == getActiveDownstream();
    }


    @Override
    protected void initSelf(@Nullable Bundle savedState) {
        upstream.onActive(true);
    }

    @Override
    protected void initDownstream(final @Nullable Bundle savedState) {
        final int n = downstreamStates.length;
        for (int i = 0; i < n; ++i) {
            final DownstreamState state = downstreamStates[i];
            state.downstream.init(new MessageControlChannel() {
                @Override
                public void onActive(boolean active) {
                    state.upActive = active;
                    setActiveDownstream();
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
                    return MultiNode.this.getMessageControlState();
                }

                @Override
                public void post(Runnable r) {
                    MultiNode.this.post(r);
                }

                @Override
                public void postDelayed(Runnable r, int delayMs) {
                    MultiNode.this.postDelayed(r, delayMs);
                }

                @Override
                public Scheduler getScheduler() {
                    return MultiNode.this.getScheduler();
                }
            }, savedState);
        }
    }

    @Override
    public void onActive(boolean active) {
        this.active = active;
        setActiveDownstream();
        // TODO on inactive with pending, should pending be sent back upstream?
    }

    @Override
    public void onMessageControl(MessageControl mc) {
        @Nullable MessageControlNode activeDownstream = getActiveDownstream();
        if (null != activeDownstream) {
            activeDownstream.onMessageControl(mc);
        } else {
            // append to the queue for when the active state flips, see #setActiveDownstream
            pendingMessageControls.add(mc);
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
