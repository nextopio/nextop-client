package io.nextop.client.node.nextop;

import com.google.common.io.ByteStreams;
import io.nextop.Id;
import io.nextop.Message;
import io.nextop.Wire;
import io.nextop.WireValue;
import io.nextop.client.MessageContext;
import io.nextop.client.MessageControl;
import io.nextop.client.MessageControlNode;
import io.nextop.client.MessageControlState;
import io.nextop.client.node.AbstractMessageControlNode;
import io.nextop.log.NL;
import io.nextop.util.NoCopyByteArrayOutputStream;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Nextop is symmetric protocol, so the client and server both use an instance
 * of this class to communicate. The difference between instances is the
 * Wire.Factory, which is responsible for a secure connection.
 * The nextop protocol is optimized to pipeline messages up/down. There is a tradeoff
 * in ordering in the case the endpoint crashes. Assuming a reliable endpoint, order is maintained. */
public class NextopNode extends AbstractMessageControlNode {

    public static final class Config {
        public final int chunkBytes;

        public Config(int chunkBytes) {
            this.chunkBytes = chunkBytes;
        }
    }


    public static final Config DEFAULT_CONFIG = new Config(/* aim for one packet per chunk */ 4 * 1024);

    public static final CompressionStrategy COMPRESS_NON_BINARY = new CompressionStrategy() {
        @Override
        public boolean isCompress(Message message) {
            @Nullable WireValue content = message.getContent();
            if (null == content) {
                return true;
            }
            switch (content.getType()) {
                case IMAGE:
                case BLOB:
                    return false;
                default:
                    return true;
            }
        }
    };

    private static final int DEFAULT_T_STARTUP_MS = 3000;
    private static final int DEFAULT_T_DROP_MS = 2 * DEFAULT_T_STARTUP_MS;


    final Config config;

    @Nullable
    Wire.Factory wireFactory;

    @Nullable
    volatile Wire.Adapter wireAdapter = null;

    boolean active = false;

    @Nullable
    ControlLooper controlLooper = null;


    final SharedTransferState sts;

    CompressionStrategy compressionStrategy = COMPRESS_NON_BINARY;

    final UpstreamActive upstreamActive;

    final int startupMs = DEFAULT_T_STARTUP_MS;
    final int dropTimeoutMs = DEFAULT_T_DROP_MS;



    public NextopNode() {
        this(DEFAULT_CONFIG);
    }
    public NextopNode(Config config) {
        this.config = config;

        sts = new SharedTransferState(this);

        upstreamActive = new UpstreamActive();
    }

    /** Call before #init.
     * @param wireFactory can be an instance of MessageControlNode.
     *                     in that case, it will be attached as a sub-node ({@link #initDownstream}).
     *                     this is useful if the wire factory needs to maintain its own network stack. */
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
        // the control looper sets upstream active when there is a successful connection to the peer
        // the control looper drops upstream active after #dropTimeoutMs of no successful connection to the peer
        // hold for the estimated #startupMs
        // This prevents a request from hitting another route when it will be net faster/better
        // to hit nextop if sent before this timeout
        upstreamActive.up(startupMs);
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
                mcs.add(mc);
            }
        }
        // TODO else send back upstream?
    }


    /////// CONNECTIVITY ///////

    private void onConnected() {
        upstreamActive.up();
    }

    private void onDisconnected() {
        upstreamActive.down(dropTimeoutMs);
    }


    private final Runnable ON_CONNECTED = new Runnable() {
        @Override
        public void run() {
            onConnected();
        }
    };
    private final Runnable ON_DISCONNECTED = new Runnable() {
        @Override
        public void run() {
            onDisconnected();
        }
    };


    final class UpstreamActive {
        boolean active = false;

        long pendingDownTime = 0L;
        @Nullable
        Runnable pendingDown = null;

        private void clearPendingDown() {
            pendingDownTime = 0L;
            pendingDown = null;
        }

        void up() {
            clearPendingDown();
            if (!active) {
                active = true;
                upstream.onActive(true);
            }
        }
        void down() {
            clearPendingDown();
            if (active) {
                active = false;
                upstream.onActive(false);
            }
        }

        void up(int holdMs) {
            up();
            down(holdMs);
        }
        void down(int delayMs) {
            long downTime = System.currentTimeMillis() + delayMs;
            if (pendingDownTime < downTime) {
                pendingDownTime = downTime;
                pendingDown = new Runnable() {
                    @Override
                    public void run() {
                        if (this == pendingDown) {
                            down();
                        }
                    }
                };
                postDelayed(pendingDown, delayMs);
            }
        }
    }


    final class ControlLooper extends Thread {
        final byte[] controlBuffer = new byte[4 * 1024];

        @Override
        public void run() {
            @Nullable SerializationState ss = null;
            @Nullable SharedWireState sws = null;

            top:
            while (active) {
                try {
                    if (null == sws || !sws.active) {
                        Wire wire;
                        try {
                            wire = wireFactory.create(null != sws ? sws.wire : null);
                        } catch (NoSuchElementException e) {
                            sws = null;
                            continue top;
                        }

                        Wire.Adapter wireAdapter = NextopNode.this.wireAdapter;
                        if (null != wireAdapter) {
                            wire = wireAdapter.adapt(wire);
                        }

                        {
                            long startNanos = System.nanoTime();
                            try {
                                syncTransferState(wire);
                            } catch (IOException e) {
                                // FIXME log
                                e.printStackTrace();
                                continue top;
                            }
                            NL.nl.metric("node.nextop.control.sync", System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                        }

                        post(ON_CONNECTED);

                        sws = new SharedWireState(wire);
                        if (null == ss) {
                            ss = new SerializationState();
                        }
                        WriteLooper writeLooper = new WriteLooper(sws, ss);
                        ReadLooper readLooper = new ReadLooper(sws);
                        sws.writeLooper = writeLooper;
                        sws.readLooper = readLooper;

                        writeLooper.start();
                        readLooper.start();

                    } // else it was just an interruption

                    try {
                        sws.awaitEnd();
                    } catch (InterruptedException e) {
                        continue top;
                    }

                } catch (Exception e) {
                    // FIXME log
                    e.printStackTrace();
                    continue top;
                }
                finally {
                    post(ON_DISCONNECTED);
                }
            }

            if (null != sws) {
                sws.end();
                while (true) {
                    try {
                        sws.awaitEnd();
                        break;
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
            }
        }


        // FIXME see notes in SharedTransferState
        void syncTransferState(final Wire wire) throws IOException {
            sts.membar();

            // each side sends SharedTransferState (id->transferred chunks)
            // each side removes parts of the shared transfer state that the other side does not have

            // FIXME

            // send write state header
            // receive other side write state header
            // loop where write and read interleave

            // make changes to read state using other side write state
            // make changes to write state using other side read state


            // TODO what are the most important changes to make?
            // TODO this only matters when ACK is moved to complete not receive (there is a "hanging" message that both sides acknowledge)
            // - if any in pendingWrite that are not in pendingRead, move from pendingWrite to write
            // - if any are in pendingRead that are not in pendingWrite,

            // TODO for now, just remove any readState that is not in the other writeState. this frees up memory

            // F_SYNC_WRITE_STATE [frame count] [frame+]
            // frame := [id]

            final int n = sts.writeStates.size();
            final int m;
            {
                int c = 0;
                {
                    controlBuffer[c] = F_SYNC_WRITE_STATE;
                    c += 1;
                    WireValue.putint(controlBuffer, c, n);
                    c += 4;
                    wire.write(controlBuffer, 0, c, 0);
                    wire.flush();
                }
                wire.read(controlBuffer, 0, c, 0);

                c = 0;
                if (F_SYNC_WRITE_STATE != controlBuffer[c]) {
                    // FIXME log
                    throw new IOException("Bad sync header.");
                }
                c += 1;
                m = WireValue.getint(controlBuffer, c);
            }


            final int bytesPerFrame = Id.LENGTH;

            // write
            class Writer extends Thread {
                int i = 0;
                Iterator<MessageWriteState> itr = sts.writeStates.values().iterator();

                @Nullable IOException e = null;

                @Override
                public void run() {
                    try {
                        for (int writeCount; 0 < (writeCount = Math.min(n - i, controlBuffer.length / bytesPerFrame)); ) {
                            for (int k = 0; k < writeCount; ++k) {
                                MessageWriteState writeState = itr.next();
                                Id.toBytes(writeState.id, controlBuffer, k * bytesPerFrame);
                            }
                            wire.write(controlBuffer, 0, writeCount * bytesPerFrame, 0);
                            i += writeCount;
                        }
                        wire.flush();
                    } catch (IOException e) {
                        // FIXME log
                        e.printStackTrace();
                        this.e = e;
                    }
                }
            };
            Writer writer = new Writer();
            writer.start();

            // read
            int j = 0;
            Id[] pairs = new Id[m];

            for (int readCount; 0 < (readCount = Math.min(m - j, controlBuffer.length / bytesPerFrame)); ) {
                wire.read(controlBuffer, 0, readCount * bytesPerFrame, 0);

                for (int k = 0; k < readCount; ++k) {
                    Id id = Id.fromBytes(controlBuffer, k * bytesPerFrame);
                    pairs[j + k] = id;
                }

                j += readCount;
            }

            // process read pairs
            // remove any read state that does not have a pair id
            sts.readStates.keySet().retainAll(Arrays.asList(pairs));


            while (true) {
                try {
                    writer.join();
                    break;
                } catch (InterruptedException e) {
                    // can't interrupt io
                    continue;
                }
            }
            if (null != writer.e) {
                throw writer.e;
            }


            // end
            {
                int c = 0;
                {
                    controlBuffer[c] = F_SYNC_END;
                    c += 1;
                    controlBuffer[c] = SYNC_STATUS_OK;
                    c += 1;
                    wire.write(controlBuffer, 0, c, 0);
                    wire.flush();
                }
                wire.read(controlBuffer, 0, c, 0);

                c = 0;
                if (F_SYNC_END != controlBuffer[c]) {
                    // FIXME log
                    throw new IOException("Bad sync end.");
                }
                c += 1;
                if (SYNC_STATUS_OK != controlBuffer[c]) {
                    // FIXME log
                    throw new IOException("Bad sync status.");
                }
            }

            sts.membar();
        }
    }

    /* nextop framed format:
     * [byte type][next bytes depend on type] */

    // FIXME finish
    static final class SharedWireState {
        final Wire wire;
        volatile boolean active = true;

        WriteLooper writeLooper;
        ReadLooper readLooper;


        SharedWireState(Wire wire) {
            this.wire = wire;
        }

        void end() {
            synchronized (this) {
                active = false;
                notifyAll();
            }
            writeLooper.interrupt();
            readLooper.interrupt();
        }

        void awaitEnd() throws InterruptedException {
            synchronized (this) {
                while (active) {
                    wait();
                }
            }
            writeLooper.join();
            readLooper.join();
        }
    }

    static final class SerializationState {
        // FIXME need to work more on memory footprint.
        final byte[] serBytes = new byte[8 * 1024 * 1024];
        final ByteBuffer serBuffer = ByteBuffer.wrap(serBytes);

        SerializationState() {
        }
    }

    final class WriteLooper extends Thread {
        final SharedWireState sws;
        final SerializationState ss;
        final MessageControlState mcs = getMessageControlState();

        final byte[] controlBuffer = new byte[1024];

        WriteLooper(SharedWireState sws, SerializationState ss) {
            this.sws = sws;
            this.ss = ss;
        }

        @Override
        public void run() {
            sts.membar();

            // take top
            // write
            // every chunkQ, check if there if a more important, before writing the next chunk
            // if so put back

            @Nullable MessageControlState.Entry entry = null;

            try {
                NL.nl.message("node.nextop.write", "Start write loop");

                top:
                while (sws.active) {
                    pollUrgent();

                    if (null == entry) {
                        entry = mcs.takeFirstAvailable(NextopNode.this);
                        if (null == entry) {
                            {
                                sws.wire.flush();
                            }
                            try {
                                entry = mcs.takeFirstAvailable(NextopNode.this, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
                                assert null != entry;
                                if (null == entry) {
                                    continue;
                                }
                            } catch (InterruptedException e) {
                                continue;
                            }
                        }
                    }

                    @Nullable MessageWriteState writeState = sts.writeStates.get(entry.id);
                    if (null == writeState) {
                        {
                            long startNanos = System.nanoTime();

                            final ByteBuffer serBuffer = ss.serBuffer;
                            final byte[] serBytes = ss.serBytes;

                            // create it
                            byte[] bytes;
                            boolean compressed;
                            try {
                                pkg(entry.mc).toBytes(serBuffer);
                                serBuffer.flip();
                                int n = serBuffer.remaining();

                                assert pkg(entry.mc).equals(WireValue.valueOf(serBytes));

                                if (compressionStrategy.isCompress(entry.message)) {
                                    try {
                                        NoCopyByteArrayOutputStream os = new NoCopyByteArrayOutputStream(serBytes, n);
                                        GZIPOutputStream gzos = new GZIPOutputStream(os);
                                        try {
                                            gzos.write(serBytes, 0, n);
                                            gzos.finish();
                                        } finally {
                                            gzos.close();
                                        }
                                        bytes = os.toByteArray();
                                        compressed = true;
                                    } catch (OutOfMemoryError e) {
                                        // Don't compress - too much temp space needed
                                        bytes = new byte[n];
                                        System.arraycopy(serBytes, 0, bytes, 0, n);
                                        compressed = false;
                                    }
                                } else {
                                    bytes = new byte[n];
                                    System.arraycopy(serBytes, 0, bytes, 0, n);
                                    compressed = false;
                                }
                            } finally {
                                serBuffer.clear();
                            }

                            assert 0 < bytes.length;

                            int chunkCount = (bytes.length + config.chunkBytes - 1) / config.chunkBytes;
                            int[] chunkOffsets = new int[chunkCount];
                            chunkOffsets[0] = 0;
                            for (int i = 1; i < chunkCount; ++i) {
                                chunkOffsets[i] = chunkOffsets[i - 1] + config.chunkBytes;
                            }

                            writeState = new MessageWriteState(entry.id, bytes, chunkOffsets, compressed);

                            NL.nl.metric("node.nextop.write.state", System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                            NL.nl.count("node.nextop.write.%s", entry.mc.type);
                        }
                    }

                    final int n = writeState.chunkOffsets.length;

                    // F_MESSAGE_START [id][total length][total chunks]
                    {
                        long startNanos = System.nanoTime();
                        {
                            int c = 0;
                            controlBuffer[c] = F_MESSAGE_START;
                            c += 1;
                            Id.toBytes(entry.id, controlBuffer, c);
                            c += Id.LENGTH;
                            WireValue.putint(controlBuffer, c, writeState.bytes.length);
                            c += 4;
                            WireValue.putint(controlBuffer, c, n);
                            c += 4;
                            controlBuffer[c] = writeState.compressed ? (byte) 0x01 : (byte) 0x00;
                            c += 1;
                            sws.wire.write(controlBuffer, 0, c, 0);
                        }
                        NL.nl.metric("node.nextop.write.start", System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                    }

                    for (int i = 0; i < n; ++i) {
                        pollUrgent();

                        if (!writeState.chunkWrites[i]) {
                            if (null != entry.end) {
                                // ended
                                entry = null;
                                continue top;
                            }

                            // write it
                            int start = writeState.chunkOffsets[i];
                            int end = i + 1 < n ? writeState.chunkOffsets[i + 1] : writeState.bytes.length;
                            assert start < end;

                            // F_MESSAGE_CHUNK [chunk index][chunk offset][chunk length][data]
                            {
                                long startNanos = System.nanoTime();
                                {
                                    {
                                        int c = 0;
                                        controlBuffer[c] = F_MESSAGE_CHUNK;
                                        c += 1;
                                        WireValue.putint(controlBuffer, c, i);
                                        c += 4;
                                        WireValue.putint(controlBuffer, c, start);
                                        c += 4;
                                        WireValue.putint(controlBuffer, c, end - start);
                                        c += 4;
                                        sws.wire.write(controlBuffer, 0, c, 0);
                                    }
                                    sws.wire.write(writeState.bytes, start, end - start, 0);
                                }
                                NL.nl.metric("node.nextop.write.chunk", System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                            }


                            writeState.chunkWrites[i] = true;


                            @Nullable MessageControlState.Entry preemptEntry = mcs.takeFirstAvailable(entry.id, NextopNode.this);
                            if (null != preemptEntry) {
                                mcs.release(entry.id, NextopNode.this);
                                entry = preemptEntry;
                                continue top;
                            }
                        }
                    }

                    // F_MESSAGE_END
                    {
                        long startNanos = System.nanoTime();
                        {
                            int c = 0;
                            controlBuffer[c] = F_MESSAGE_END;
                            c += 1;
                            sws.wire.write(controlBuffer, 0, c, 0);
                        }
                        NL.nl.metric("node.nextop.write.end", System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                    }

                    // done with entry, transfer to pending ack
                    mcs.remove(entry.id, MessageControlState.End.COMPLETED);
                    sts.writePendingAck.add(entry.mc);
                    entry = null;
                }

                {
                    sws.wire.flush();
                }
            } catch (IOException e) {
                // FIXME log
                e.printStackTrace();
                // fatal
                sws.end();
            }


            if (null != entry) {
                mcs.release(entry.id, NextopNode.this);
                entry = null;
            }


            NL.nl.message("node.nextop.write", "End write loop");

            sts.membar();
        }

        private void pollUrgent() throws IOException {
            {
                int u = 0;
                long startNanos = System.nanoTime();
                for (byte[] urgentMessage; null != (urgentMessage = sts.writeUrgentMessages.poll()); ) {
                    sws.wire.write(urgentMessage, 0, urgentMessage.length, 0);
                    u += 1;
                }
                if (0 < u) {
                    NL.nl.metric("node.nextop.write.urgent", System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
                }
            }
        }
    }

    final class ReadLooper extends Thread {
        final SharedWireState sws;
        final MessageControlState mcs = getMessageControlState();

        final byte[] controlBuffer = new byte[1024];


        ReadLooper(SharedWireState sws) {
            this.sws = sws;
        }


        @Override
        public void run() {

            // FIXME
            // as soon as get a COMPLETE, send an ACK (this is not resilient to crash, but works for now to keep the client buffer limited)
            // on F_MESSAGE_COMPLETE or F_MESSAGE_CHUNK, if there is a verification error, send back a NACK
            // if read NACK, move message from pendingWrite back to mcs

            sts.membar();

            @Nullable Id id = null;
            @Nullable MessageReadState readState = null;

            try {

                NL.nl.message("node.nextop.read", "Start read loop");

                top:
                while (sws.active) {
                    sws.wire.read(controlBuffer, 0, 1, 0);
                    {
                        long startNanos = System.nanoTime();
                        byte type = controlBuffer[0];
                        switch (type) {
                            case F_MESSAGE_START: {
                                // F_MESSAGE_START [id][total length][total chunks][compressed]
                                int c = Id.LENGTH + 4 + 4 + 1;
                                sws.wire.read(controlBuffer, 0, c, 0);
                                c = 0;
                                id = Id.fromBytes(controlBuffer, c);
                                c += Id.LENGTH;
                                int length = WireValue.getint(controlBuffer, c);
                                c += 4;
                                int chunkCount = WireValue.getint(controlBuffer, c);
                                c += 4;
                                boolean compressed = (0xFF & controlBuffer[c]) != 0;

                                readState = sts.readStates.get(id);
                                if (null == readState) {
                                    // create it
                                    readState = new MessageReadState(id, length, chunkCount, compressed);
                                    sts.readStates.put(id, readState);
                                }

                                break;
                            }
                            case F_MESSAGE_CHUNK: {
                                if (null == readState) {
                                    continue top;
                                }

                                // F_MESSAGE_CHUNK [chunk index][chunk offset][chunk length][data]
                                int c = 4 + 4 + 4;
                                sws.wire.read(controlBuffer, 0, c, 0);
                                c = 0;
                                int chunkIndex = WireValue.getint(controlBuffer, c);
                                c += 4;
                                int start = WireValue.getint(controlBuffer, c);
                                c += 4;
                                int chunkLength = WireValue.getint(controlBuffer, c);

                                int end = start + chunkLength;

                                // verify that the values do not conflict with existing values
                                // designed so that each index passing verification implies that the entire read state is valid
                                boolean conflict = false;
                                try {
                                    if (readState.chunkReads[chunkIndex]) {
                                        // already read
                                        conflict = false;
                                    } else {
                                        if (0 <= chunkIndex - 1 && readState.chunkReads[chunkIndex - 1]) {
                                            // the previous chunk was read and set the index of the current chunk
                                            if (start != readState.chunkOffsets[chunkIndex]) {
                                                // index does not match value set in previous chunk
                                                conflict = true;
                                            }
                                        }

                                        if (chunkIndex + 1 < readState.chunkOffsets.length) {
                                            if (readState.chunkReads[chunkIndex] && end != readState.chunkOffsets[chunkIndex + 1]) {
                                                // end does not match known
                                                conflict = true;
                                            }
                                        } else {
                                            if (end != readState.bytes.length) {
                                                // end does not match known
                                                conflict = true;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // FIXME log
                                    e.printStackTrace();
                                    // index out of bounds, etc
                                    conflict = true;
                                }

                                if (conflict) {
                                    NL.nl.count("node.nextop.read.conflict");

                                    // discard chunk content
                                    sws.wire.skip(chunkLength, 0);

                                    // FIXME log this
                                    sts.writeUrgentMessages.add(nack(id));
                                    sws.writeLooper.interrupt();
                                    // discard the read state
                                    sts.readStates.remove(id);
                                    continue top;
                                }

                                // read chunk content
                                sws.wire.read(readState.bytes, start, chunkLength, 0);

                                readState.chunkReads[chunkIndex] = true;
                                readState.chunkOffsets[chunkIndex] = start;
                                if (chunkIndex + 1 < readState.chunkOffsets.length) {
                                    // set the next start, used for conflict detection (see above)
                                    readState.chunkOffsets[chunkIndex + 1] = end;
                                }

                                break;
                            }
                            case F_MESSAGE_END: {
                                if (null == readState) {
                                    continue top;
                                }

                                // F_MESSAGE_END
                                // nothing to read

                                for (int i = 0, n = readState.chunkOffsets.length; i < n; ++i) {
                                    if (!readState.chunkReads[i]) {
                                        sts.writeUrgentMessages.add(nack(id));
                                        sws.writeLooper.interrupt();
                                        // discard the read state
                                        sts.readStates.remove(id);
                                        readState = null;
                                        continue top;
                                    }
                                }

                                // received
                                // TODO move this to where the message is actually completed (ack on complete not receive)
                                // TODO when ack changed, move message to readPending
                                sts.writeUrgentMessages.offer(ack(id));
                                sws.writeLooper.interrupt();

                                sts.readStates.remove(id);

                                // defer the parsing to the context thread
                                // TODO is this better thank inline? (get numbers)
                                post(new Dispatch(id, readState));
                                readState = null;

                                break;
                            }
                            case F_ACK: {
                                // F_ACK [id]
                                int c = Id.LENGTH;
                                sws.wire.read(controlBuffer, 0, c, 0);
                                c = 0;
                                Id uid = Id.fromBytes(controlBuffer, c);

                                // remove from pending
                                sts.writePendingAck.remove(uid, MessageControlState.End.COMPLETED);

                                break;
                            }
                            case F_NACK: {
                                NL.nl.count("node.nextop.read.nack");

                                // F_NACK [id]
                                int c = Id.LENGTH;
                                sws.wire.read(controlBuffer, 0, c, 0);
                                c = 0;
                                Id uid = Id.fromBytes(controlBuffer, c);

                                // move from pending to active
                                @Nullable MessageControl mc = sts.writePendingAck.remove(uid, MessageControlState.End.ERROR);
                                if (null != mc) {
                                    mcs.add(mc);
                                } else {
                                    // this would be a bug in sync state - one node thought the other had something it doesn't
                                    assert false;
                                }

                                break;
                            }
                            default:
                                // protocol error
                                throw new IOException("Protocol error.");
                        }
                        NL.nl.metric("node.nextop.read.%s", System.nanoTime() - startNanos, TimeUnit.NANOSECONDS, type);
                    }

                }
            } catch (IOException e) {
                // FIXME log
                e.printStackTrace();
                // fatal
                sws.end();
            }

            NL.nl.message("node.nextop.read", "End read loop");

            sts.membar();
        }

        final class Dispatch implements Runnable {
            final Id id;
            final MessageReadState readState;

            Dispatch(Id id, MessageReadState readState) {
                this.id = id;
                this.readState = readState;
            }

            @Override
            public void run() {
                try {
                    WireValue pkg;
                    if (readState.compressed) {
                        NoCopyByteArrayOutputStream os = new NoCopyByteArrayOutputStream(1024);
                        ByteStreams.copy(new GZIPInputStream(new ByteArrayInputStream(readState.bytes)), os);
                        // TODO copy this if the overhead is too large (the byte buffer has padding at the end)
                        pkg = WireValue.valueOf(os.getBytes(), os.getOffset());
                    } else {
                        pkg = WireValue.valueOf(readState.bytes);
                    }

                    MessageControl mc = unpkg(pkg);
                    NL.nl.count("node.nextop.read.%s", mc.type);
                    upstream.onMessageControl(MessageControl.receive(mc.type, mc.message));
                } catch (Exception e) {
                    // FIXME the nack might create an infinite retry here; think about something better
                    // FIXME possibly just ban the session since it's running an incompatible version
                    sts.writeUrgentMessages.add(nack(id));
                    sws.writeLooper.interrupt();

                    NL.nl.unhandled("node.nextop.read", e);
                }
            }
        }
    }

    // urgent messages

    static byte[] nack(Id id) {
        // F_NACK [id]
        byte[] nack = new byte[1 + Id.LENGTH];
        int c = 0;
        nack[c] = F_NACK;
        c += 1;
        Id.toBytes(id, nack, c);
        return nack;
    }

    static byte[] ack(Id id) {
        // F_NACK [id]
        byte[] ack = new byte[1 + Id.LENGTH];
        int c = 0;
        ack[c] = F_ACK;
        c += 1;
        Id.toBytes(id, ack, c);
        return ack;
    }

    // message packaging

    static WireValue pkg(MessageControl mc) {
        return MessageControl.toWireValue(mc);
    }

    static MessageControl unpkg(WireValue value) {
        return MessageControl.fromWireValue(value);
    }


    // FIXME relied on new threads being a membar. all this state is shared across 1+1 (writer+reader) threads in sequence
    static final class SharedTransferState {

        // when a message is remove from the shared mcs on write, it goes here
        // these message are pendinging ack
        // sync state established which of these are still valid. if any not valid, the client immediately retransmits at the front of the line
        // the nextop node holds these even if the node goes active->false. the protocol is set up that on reconnect they will get sent.
        //    even if a billing outage, getting these sent is an exception - they will always get sent even if the account is in bad standing etc.
        MessageControlState writePendingAck;

        // TODO this matters when the node reads and dispatches, waiting for a complete back
        // TODO store here until the complete/ack (so the message isn't lost)
//        MessageControlState readPendingAck;

        /** single-thread */
        Map<Id, MessageWriteState> writeStates;


        /** single-thread */
        Map<Id, MessageReadState> readStates;


        /** thread-safe */
        Queue<byte[]> writeUrgentMessages;



        SharedTransferState(MessageContext context) {
            writePendingAck = new MessageControlState(context);
//            readPendingAck = new MessageControlState(context);

            writeStates = new HashMap<Id, MessageWriteState>(32);
            readStates = new HashMap<Id, MessageReadState>(32);

            writeUrgentMessages = new ConcurrentLinkedQueue<byte[]>();
        }


        synchronized void membar() {

        }
    }

    static final class MessageWriteState {
        final Id id;

        final byte[] bytes;
        final boolean compressed;
        // [0] is the start of the first chunk
        final int[] chunkOffsets;
        final boolean[] chunkWrites;


        MessageWriteState(Id id, byte[] bytes, int[] chunkOffsets, boolean compressed) {
            this.id = id;
            this.bytes = bytes;
            this.chunkOffsets = chunkOffsets;
            this.compressed = compressed;
            // init all false
            chunkWrites = new boolean[chunkOffsets.length];
        }
    }

    static final class MessageReadState {
        final Id id;
        final boolean compressed;

        final byte[] bytes;
        // [0] is the start of the first chunk
        final int[] chunkOffsets;
        final boolean[] chunkReads;


        MessageReadState(Id id, int length, int chunkCount, boolean compressed) {
            if (length < chunkCount) {
                throw new IllegalArgumentException();
            }
            this.id = id;
            this.compressed = compressed;
            bytes = new byte[length];
            chunkOffsets = new int[chunkCount];
            chunkReads = new boolean[chunkCount];
        }
    }



    /////// NEXTOP PROTOCOL ///////

    // FIXME be able to transfer MessageControl not just message

    /** [id][total length][total chunks][compressed] */
    public static final byte F_MESSAGE_START = 0x01;
    /** [chunk index][chunk offset][chunk length][data] */
    public static final byte F_MESSAGE_CHUNK = 0x02;
    /** TODO checksum */
    public static final byte F_MESSAGE_END = 0x03;

    /** [id]
     * ack indicates the node can remove its copy of the message. */
    static final byte F_ACK = 0x04;
    /** [id]
     * nack indicates the node should resend its copy of the message */
    static final byte F_NACK = 0x05;

    /** [frame count][frame+]
     * frame := [id] */
    static final byte F_SYNC_WRITE_STATE = 0x70;

    /** [status]
     * status is a single byte, SYNC_STATUS_OK, SYNC_STATUS_ERROR */
    static final byte F_SYNC_END = 0x70;
    static final byte SYNC_STATUS_OK = 0x00;
    static final byte SYNC_STATUS_ERROR = 0x01;




    public static interface CompressionStrategy {
        boolean isCompress(Message message);
    }



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
