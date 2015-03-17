package io.nextop.wire;

import io.nextop.Wire;

import java.io.IOException;
import java.util.NoSuchElementException;

public class Probe implements Wire.Adapter {

    final Object mutex = new Object();

    int wireCount = 0;
    int errorCount = 0;
    long readBytes = 0L;
    long writeBytes = 0L;


    public Probe() {
    }


    public int getWireCount() {
        synchronized (mutex) {
            return wireCount;
        }
    }

    public int getErrorCount() {
        synchronized (mutex) {
            return errorCount;
        }
    }

    public long getReadBytes() {
        synchronized (mutex) {
            return readBytes;
        }
    }

    public long getWriteBytes() {
        synchronized (mutex) {
            return writeBytes;
        }
    }


    /////// Wire.Adapter ///////

    @Override
    public Wire adapt(Wire wire) throws InterruptedException, NoSuchElementException {
        ProbedWire pw = new ProbedWire(wire);
        synchronized (mutex) {
            wireCount += 1;
        }
        return pw;
    }


    final class ProbedWire implements Wire {
        final Wire impl;


        ProbedWire(Wire impl) {
            this.impl = impl;
        }


        void error (IOException e) throws IOException {
            synchronized (mutex) {
                errorCount += 1;
            }
            throw e;
        }


        @Override
        public void close() throws IOException {
            try {
                impl.close();
            } catch (IOException e) {
                error(e);
            }
        }
        @Override
        public void read(byte[] buffer, int offset, int length, int messageBoundary) throws IOException {
            try {
                impl.read(buffer, offset, length, messageBoundary);
                synchronized (mutex) {
                    readBytes += length;
                }
            } catch (IOException e) {
                error(e);
            }
        }
        @Override
        public void skip(long n, int messageBoundary) throws IOException {
            try {
                impl.skip(n, messageBoundary);
                synchronized (mutex) {
                    readBytes += n;
                }
            } catch (IOException e) {
                error(e);
            }
        }
        @Override
        public void write(byte[] buffer, int offset, int length, int messageBoundary) throws IOException {
            try {
                impl.write(buffer, offset, length, messageBoundary);
                synchronized (mutex) {
                    writeBytes += length;
                }
            } catch (IOException e) {
                error(e);
            }
        }
        @Override
        public void flush() throws IOException {
            try {
                impl.flush();
            } catch (IOException e) {
                error(e);
            }
        }
    }

}
