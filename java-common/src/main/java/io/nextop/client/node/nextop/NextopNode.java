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

/** Nextop is symmetric protocol, so the client and server both use an instance
 * of this class to communicate. The difference between instances is the
 * Wire.Factory, which is responsible for a secure connection. */
public class NextopNode extends AbstractMessageControlNode {

    public static final class Config {

    }


    final Config config;

    @Nullable
    Wire.Factory wireFactory;

    @Nullable
    volatile Wire.Adapter wireAdapter = null;

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






    final class ControlLooper extends Thread {
        SharedTransferState sts;


        @Override
        public void run() {

            @Nullable SharedWireState sws = null;


            while (active) {
                try {
                    if (null == sws || !sws.active) {
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
                        WriteLooper writeLooper = new WriteLooper(sws);
                        ReadLooper readLooper = new ReadLooper(sws);
                        sws.writeLooper = writeLooper;
                        sws.readLooper = readLooper;
                        writeLooper.start();
                        readLooper.start();

                    } // else it was just an interruption

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
            // each side sends SharedTransferState (id->transferred chunks)
            // each side removes parts of the shared transfer state that the other side does not have

            // FIXME

        }
    }

    /* nextop framed format:
     * [byte type][next bytes depend on type] */

    static final class SharedWireState {
        final Wire wire;
        volatile boolean active;

        WriteLooper writeLooper;
        ReadLooper readLooper;

        SharedWireState(Wire wire) {
            this.wire = wire;
        }


        void end() {
            // interrupt writer, reader
            active = false;
            writeLooper.interrupt();
            readLooper.interrupt();

        }

        void awaitEnd() throws InterruptedException {

        }
    }

    final class WriteLooper extends Thread {
        final SharedWireState sws;
        final MessageControlState mcs = getMessageControlState();

        WriteLooper(SharedWireState sws) {
            this.sws = sws;
        }


        @Override
        public void run() {


            // take top
            // write
            // every chunkQ, check if there if a more important, before writing the next chunk
            // if so put back


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




    static final class SharedTransferState {
        // FIXME active session ID

        // FIXME ignore this for now
        // TODO on end of control looper, release these messages back into the upstream
//        Map<Id, Message> pendingAck;

        // write
        // id -> MessageWriteState (bytes, transferred chunks)

        // read
        // id -> MessageReadState

        // FIXME for each pending, rx listen to mcs for cancel/end
    }

    static final class MessageWriteState {
        Id id;

        byte[] bytes;
        // [0] is the start of the first chunk
        int[] chunkOffsets;
        boolean[] chunkWrites;


    }

    static final class MessageReadState {
        Id id;

        byte[] bytes;
        // [0] is the start of the first chunk
        int[] chunkOffsets;
        boolean[] chunkReads;

    }





    /////// NEXTOP PROTOCOL ///////

    // FIXME be able to transfer MessageControl not just message
    /** [id][total length][total chunks] */
    public static final byte F_START_MESSAGE = 0x01;
    /** [chunk index][chunk offset][chunk length][data] */
    public static final byte F_MESSAGE_CHUNK = 0x02;
    /** [md5] */
    public static final byte F_MESSAGE_END = 0x03;

    // FIXME next step, ack

    // CANCEL [id]
    /** [id] */
//    static final byte F_ACK = 0x04;




    // TODO work out a more robust fallback
    // big assumption for ordering: nextop endpoint will not crash
    // compromise: maintain order and never lose a message if this is true
    // if not true, at least never lose a message (but order will be lost)

    // two phase dev:
    // (current) phase 1: just get it working, buggy in some cases, no reordering, etc
    // phase 2: correctness (never lose), reordering, etc, focus on perf






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
