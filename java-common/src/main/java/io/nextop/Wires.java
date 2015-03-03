package io.nextop;

import javax.annotation.Nullable;
import java.io.*;
import java.net.Socket;

public final class Wires {


    public static Wire io(Socket socket) throws IOException {
        socket.setTcpNoDelay(true);
        int sendBufferSize = 4 * 1024;
        OutputStream os = socket.getOutputStream();
        if (0 < sendBufferSize) {
            os = new BufferedOutputStream(os, sendBufferSize);
        }
        return io(socket.getInputStream(), os);
    }

    public static Wire io(@Nullable InputStream is, @Nullable OutputStream os) {
        return new IoWire(is, os);
    }

    public static InputStream inputStream(Wire wire) {
        return new WireInputStream(wire);
    }

    public static OutputStream outputStream(Wire wire) {
        return new WireOutputStream(wire);
    }

    public static Wire transfer() {
        return transfer(/* default tcp window */ 64 * 1024);
    }
    public static Wire transfer(int size) {
        return new TransferBuffer(size);
    }




    private Wires() {
    }


    private static final class IoWire implements Wire {
        @Nullable
        private final InputStream is;
        @Nullable
        private final OutputStream os;

        private boolean closed = false;


        IoWire(@Nullable InputStream is, @Nullable OutputStream os) {
            this.is = is;
            this.os = os;
        }


        @Override
        public void close() throws IOException {
            if (closed) {
                throw new IOException();
            }

            closed = true;
            try {
                is.close();
            } finally {
                os.close();
            }
        }

        @Override
        public void read(byte[] buffer, int offset, int length, int messageBoundary) throws IOException {
            if (closed) {
                throw new IOException();
            }
            try {
                if (null != is) {
                    int i = 0;
                    for (int r; i < length && 0 < (r = is.read(buffer, offset + i, length - i)); ) {
                        i += r;
                    }
                    if (i != length) {
                        throw new IOException();
                    }
                } else {
                    throw new IOException("No input.");
                }
            } catch (IOException e) {
                close();
                throw e;
            }
        }

        @Override
        public void skip(long n, int messageBoundary) throws IOException {
            if (closed) {
                throw new IOException();
            }
            try {
                if (null != is) {
                    long i = 0;
                    for (long r; i < n && 0 < (r = is.skip(n - i)); ) {
                        i += r;
                    }
                    if (i != n) {
                        throw new IOException();
                    }
                } else {
                    throw new IOException("No input.");
                }
            } catch (IOException e) {
                close();
                throw e;
            }
        }

        @Override
        public void write(byte[] buffer, int offset, int length, int messageBoundary) throws IOException {
            if (closed) {
                throw new IOException();
            }
            try {
                if (null != os) {
                    os.write(buffer, offset, length);
                } else {
                    throw new IOException("No output.");
                }
            } catch (IOException e) {
                close();
                throw e;
            }
        }

        @Override
        public void flush() throws IOException {
            try {
                if (null != os) {
                    os.flush();
                } else {
                    throw new IOException("No output.");
                }
            } catch (IOException e) {
                close();
                throw e;
            }
        }
    }

    private static final class WireInputStream extends InputStream {
        private final Wire wire;

        private final byte[] one = new byte[1];


        WireInputStream(Wire wire) {
            this.wire = wire;
        }


        @Override
        public int read() throws IOException {
            int c = read(one, 0, 1);
            if (0 < c) {
                assert 1 == c;
                return 0xFF & one[0];
            } else {
                return -1;
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int length) throws IOException {
            wire.read(b, off, length, 0);
            return length;
        }

        @Override
        public long skip(long n) throws IOException {
            wire.skip(n, 0);
            return n;
        }

        @Override
        public void close() throws IOException {
            wire.close();
        }
    }

    private static final class WireOutputStream extends OutputStream {
        private final Wire wire;

        private final byte[] one = new byte[1];


        WireOutputStream(Wire wire) {
            this.wire = wire;
        }


        @Override
        public void write(int b) throws IOException {
            one[0] = (byte) b;
            write(one, 0, 1);
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            wire.write(b, off, len, 0);
        }

        @Override
        public void flush() throws IOException {
            wire.flush();
        }

        @Override
        public void close() throws IOException {
            wire.close();
        }
    }



    // circular buffer
    private static final class TransferBuffer implements Wire {

        private final byte[] tb;
        private int writeIndex = 0;
        // read index trails the write index
        // available = (writeIndex - readIndex) % n = (writeIndex - readIndex + n) % n
        private int readIndex = 0;
        private int available = 0;

        private boolean closed = false;


        TransferBuffer(int size) {
            this.tb = new byte[size];
        }


        @Override
        public synchronized void close() throws IOException {
            if (closed) {
                throw new IOException();
            }
            closed = true;
        }

        @Override
        public synchronized void read(byte[] buffer, int offset, int length, int messageBoundary) throws IOException {
            int i = 0;
            while (!closed && i < length) {
                int a = Math.min(length - i, available);

                if (0 < a) {
                    for (int j = 0; j < a; ++j) {
                        buffer[offset + i + j] = tb[(readIndex + j) % tb.length];
                    }
                    i += a;
                    readIndex += a;
                    available -= a;

                    notifyAll();

                    if (length <= i) {
                        break;
                    }
                }

                try {
                    wait();
                } catch (InterruptedException e) {
                    // can't interrupt io
                }
            }
            if (closed) {
                throw new IOException();
            }
        }

        @Override
        public synchronized void skip(long n, int messageBoundary) throws IOException {
            long i = 0;
            while (!closed && i < n) {
                long a = Math.min(n - i, available);

                if (0 < a) {
                    i += a;
                    readIndex += a;
                    available -= a;

                    notifyAll();

                    if (n <= i) {
                        break;
                    }
                }

                try {
                    wait();
                } catch (InterruptedException e) {
                    // can't interrupt io
                }
            }
            if (closed) {
                throw new IOException();
            }
        }

        @Override
        public synchronized void write(byte[] buffer, int offset, int length, int messageBoundary) throws IOException {
            int i = 0;
            while (!closed && i < length) {
                int a = Math.min(length - i, tb.length - available);

                if (0 < a) {
                    for (int j = 0; j < a; ++j) {
                        tb[(writeIndex + j) % tb.length] = buffer[offset + i + j];
                    }
                    i += a;
                    writeIndex += a;
                    available += a;

                    notifyAll();

                    if (length <= i) {
                        break;
                    }
                }

                try {
                    wait();
                } catch (InterruptedException e) {
                    // can't interrupt io
                }
            }
            if (closed) {
                throw new IOException();
            }
        }

        @Override
        public synchronized void flush() throws IOException {
            // already flushed
        }
    }

}
