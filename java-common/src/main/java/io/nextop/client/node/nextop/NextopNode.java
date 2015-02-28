package io.nextop.client.node.nextop;

import io.nextop.Id;
import io.nextop.Message;
import io.nextop.client.MessageControl;
import io.nextop.client.MessageControlState;
import io.nextop.client.Wire;
import io.nextop.client.node.AbstractMessageControlNode;
import io.nextop.client.node.Head;
import io.nextop.client.retry.SendStrategy;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

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

    volatile NextopRemoteWireFactory wireFactory;
    @Nullable
    volatile Wire.Factory wireAdapterFactory = null;

    SendStrategy retakeStrategy;

    boolean active;

    @Nullable
    ControlLooper controlLooper = null;


    // FIXME set to an internal httpnode used for dns lookups
    Head dnsHead;


    // FIXME
    Id accessKey = Id.create();
    Set<Id> grantKeys = Collections.emptySet();



    public NextopNode(Config config) {
        this.config = config;
    }



    public void setWireAdapterFactory(Wire.Factory wireAdapterFactory) {
        this.wireAdapterFactory = wireAdapterFactory;
    }





    /////// NODE ///////

    @Override
    protected void initSelf(@Nullable Bundle savedState) {
        // FIXME create wire factory

        // ready to receive
        upstream.onActive(true);
    }

    @Override
    public void onActive(boolean active) {
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
        } else {
            // return to sender
            upstream.onMessageControl(mc);
        }
    }





    static final class SharedTransferState {
        // FIXME ignore this for now
        // TODO on end of control looper, release these messages back into the upstream
//        Map<Id, Message> pendingAck;
    }


    final class ControlLooper extends Thread {

        @Override
        public void run() {
            // FIXME retake timeout, create Wire, initial handshake, initial state sync
        }
    }

    /* nextop framed format:
     * [byte type][next bytes depend on type] */

    final class WriteLooper extends Thread {
        Wire wire;

        @Override
        public void run() {

        }
    }

    final class ReadLooper extends Thread {
        Wire wire;


        @Override
        public void run() {

        }
    }


    /** [id] */
    static final byte F_START_MESSAGE = 0x01;
    /** [int length][data] */
    static final byte F_MESSAGE_CHUNK = 0x02;
    /** [md5] */
    static final byte F_MESSAGE_END = 0x03;
    /** [id] */
//    static final byte F_ACK = 0x04;





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
