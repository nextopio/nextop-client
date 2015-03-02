package io.nextop.client;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

// provides two wire factories, a and b, such that a wire created in a produces an available wire in b (and vice-versa)
// the pair of wires write to each other, via a buffer of similar size to a tcp window (65k)
public final class WireFactoryPair {

    final Object mutex = new Object();
    final PairedWireFactory a;
    final PairedWireFactory b;


    public WireFactoryPair() {
        a = new PairedWireFactory(mutex);
        b = new PairedWireFactory(mutex);
        a.pair = b;
        b.pair = a;
    }


    public Wire.Factory getA() {
        return a;
    }

    public Wire.Factory getB() {
        return b;
    }


    static final class PairedWireFactory implements Wire.Factory {
        final Object mutex;
        final Queue<Wire> wireQueue = new LinkedList<Wire>();

        PairedWireFactory pair;


        PairedWireFactory(Object mutex) {
            this.mutex = mutex;
        }


        @Override
        public Wire create(@Nullable Wire replace) throws InterruptedException, NoSuchElementException {
            synchronized (mutex) {
                // 1. check if the pair created one
                @Nullable Wire wire = wireQueue.poll();
                if (null != wire) {
                    return wire;
                }

                // 2. create a pair
                Wire a = Wires.transfer();
                Wire b = Wires.transfer();

                wire = new PairWire(a, b);
                pair.wireQueue.add(new PairWire(b, a));

                return wire;
            }
        }
    }


    static final class PairWire implements Wire {
        private final Wire in;
        private final Wire out;


        PairWire(Wire in, Wire out) {
            this.in = in;
            this.out = out;
        }


        @Override
        public void close() throws IOException {
            try {
                in.close();
            } finally {
                out.close();
            }
        }

        @Override
        public void read(byte[] buffer, int offset, int length, int messageBoundary) throws IOException {
            in.read(buffer, offset, length, messageBoundary);
        }

        @Override
        public void skip(long n, int messageBoundary) throws IOException {
            in.skip(n, messageBoundary);
        }

        @Override
        public void write(byte[] buffer, int offset, int length, int messageBoundary) throws IOException {
            out.write(buffer, offset, length, messageBoundary);
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }
    }

}
