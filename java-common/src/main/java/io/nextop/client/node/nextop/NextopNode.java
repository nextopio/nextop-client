package io.nextop.client.node.nextop;

import io.nextop.Id;
import io.nextop.Message;
import io.nextop.client.MessageControl;
import io.nextop.client.MessageControlNode;
import io.nextop.client.MessageControlState;
import io.nextop.client.Wire;
import io.nextop.client.node.AbstractMessageControlNode;
import io.nextop.client.node.Head;
import io.nextop.client.node.http.HttpNode;
import io.nextop.client.retry.SendStrategy;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

// FIXME base on a wire factory
// FIXME use a wire adapter factory
public class NextopNode extends AbstractMessageControlNode {


    public static final class Config {

    }


    // big assumption for ordering: nextop endpoint will not crash
    // compromise: maintain order and never lose a message if this is true
    // if not true, at least never lose a message (but order will be lost)

    // two phase dev:
    // (current) phase 1: just get it working, buggy in some cases, no reordering, etc
    // phase 2: correctness (never lose), reordering, etc, focus on perf


    final Config config;

    @Nullable
    Wire.Factory wireFactory;



    @Nullable
    volatile Wire.Adapter wireAdapter = null;

    SendStrategy retakeStrategy;

    boolean active;

    @Nullable
    ControlLooper controlLooper = null;




    public NextopNode(Config config) {
        this.config = config;

    }

    /** @param wireFactory can be an instance of MessageControlNode */
    public void setWireFactory(Wire.Factory wireFactory) {
        this.wireFactory = wireFactory;
    }


    public void setWireAdapter(Wire.Adapter wireAdapter) {
        this.wireAdapter = wireAdapter;
    }





    /////// NODE ///////


    @Override
    protected void initDownstream(Bundle savedState) {
        if (wireFactory instanceof MessageControlNode) {
            ((MessageControlNode) wireFactory).init(this, savedState);
        }
    }

    @Override
    protected void initSelf(@Nullable Bundle savedState) {
        // ready to receive
        upstream.onActive(true);
    }

    @Override
    public void onActive(boolean active) {
        if (active && wireFactory instanceof MessageControlNode) {
            ((MessageControlNode) wireFactory).onActive(active);
        }

        if (this.active != active) {
            this.active = active;

            if (active) {
                assert null == controlLooper;

                controlLooper = new ControlLooper();
                controlLooper.start();
            } else {
                assert null != controlLooper;

                controlLooper.interrupt();
                controlLooper = null;
            }
        }

        if (!active && wireFactory instanceof MessageControlNode) {
            ((MessageControlNode) wireFactory).onActive(active);
        }
    }

    @Override
    public void onMessageControl(MessageControl mc) {
        assert MessageControl.Direction.SEND.equals(mc.dir);

        assert active;
        if (active) {
            MessageControlState mcs = getMessageControlState();
            if (!mcs.onActiveMessageControl(mc, upstream)) {
                switch (mc.type) {
                    case MESSAGE:
                        mcs.add(mc.message);
                        break;
                    default:
                        // ignore
                        break;
                }
            }
        }
    }





    static final class SharedTransferState {
        // FIXME active session ID

        // FIXME ignore this for now
        // TODO on end of control looper, release these messages back into the upstream
//        Map<Id, Message> pendingAck;

        // FIXME for each pending, rx listen to mcs for cancel/end
    }


    final class ControlLooper extends Thread {
        SharedTransferState sts;


        @Override
        public void run() {

            @Nullable SharedWireState sws;


            while (active) {
                try {
                    if (null == sws || !sws.active) {
                        // FIXME retake

                        Wire wire;
                        try {
                            wire = wireFactory.create(null != sws ? sws.wire : null);
                        } catch (NoSuchElementException e) {
                            sws = null;
                            continue;
                        }

                        Wire.Adapter wireAdapter = NextopNode.this.wireAdapter;
                        if (null != wireAdapter) {
                            wire = wireAdapter.adapt(wire);
                        }

                        try {
                            syncTransferState(wire);
                        } catch (IOException e) {
                            // FIXME log
                            continue;
                        }

                        sws = new SharedWireState(wire);
                        new WriteLooper(sws).start();
                        new ReadLooper(sws).start();
                    }

                    try {
                        sws.awaitEnd();
                    } catch (InterruptedException e) {
                        continue;
                    }

                } catch (Exception e) {
                    // FIXME log
                    continue;
                }
            }

            if (null != sws) {
                sws.end();
            }
        }

        void syncTransferState(Wire wire) throws IOException {
            // each side sends a session ID

            // FIXME

        }
    }

    /* nextop framed format:
     * [byte type][next bytes depend on type] */

    static final class SharedWireState {
        final Wire wire;
        volatile boolean active;


        SharedWireState(Wire wire) {
            this.wire = wire;
        }


        void end() {

        }

        void awaitEnd() throws InterruptedException {

        }
    }

    final class WriteLooper extends Thread {
        final SharedWireState sws;


        WriteLooper(SharedWireState sws) {
            this.sws = sws;
        }


        @Override
        public void run() {

        }
    }

    final class ReadLooper extends Thread {
        final SharedWireState sws;


        ReadLooper(SharedWireState sws) {
            this.sws = sws;
        }


        @Override
        public void run() {

        }
    }






    // shared transfer state:
    // id -> bytes, sent index in bytes

    // socket control flow:
    // - retake timeout (use take state, time since last take, elapsed)
    // - create wire (socket) (on timeout, go to [0])
    // - initial handshakes
    // - initial state sync (sync the shared transfer state)
    // - start loopers
    // - when any loopers fails, shut down all, go to [0]
    //

    // WriteLooper
    // take off the top of mcs and write
    // have a parallel thread that peeks at the next
    // every yieldQ write, surface progress, check if there is a more urgent message
    // if so, shelve the current and switch

    // ReadLooper



    // wire format:
    // [type][length]
    // types:
    // - message start [ID]
    // - message data [bytes]
    // - message end [MD5]
    // - (verify error) (ack) (on ack, delete from shared transfer state)


}
