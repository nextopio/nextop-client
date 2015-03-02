package io.nextop.client;

import rx.functions.Action0;
import rx.subjects.BehaviorSubject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Future;

public final class Wires {

    public static Wire io(@Nullable InputStream is, @Nullable OutputStream os) {
        return new IoWire(is, os);
    }

    public static InputStream inputStream(Wire wire) {
        return new WireInputStream(wire);
    }

    public static OutputStream outputStream(Wire wire) {
        return new WireOutputStream(wire);
    }




    private Wires() {
    }


    private static final class IoWire implements Wire {
        @Nullable
        private final InputStream is;
        @Nullable
        private final OutputStream os;

        private final BehaviorSubject<IOException> exSubject;
        private final Future<IOException> exFuture;

        private boolean closed = false;


        IoWire(@Nullable InputStream is, @Nullable OutputStream os) {
            this.is = is;
            this.os = os;

            exSubject = BehaviorSubject.create();
            exFuture = exSubject.doOnUnsubscribe(new Action0() {
                @Override
                public void call() {
                    close();
                }
            }).toBlocking().toFuture();
        }


        private void close() {
            if (!closed) {
                try {
                    is.close();
                } catch (IOException e) {
                    // continue
                }
                try {
                    os.close();
                } catch (IOException e) {
                    // continue
                }
                closed = true;
            }
        }

        private void close(IOException cause) {
            exSubject.onNext(cause);
            assert closed;
        }


        @Override
        public Future<IOException> open() throws IOException {
            // already open
            return exFuture;
        }

        @Override
        public void read(byte[] buffer, int offset, int length, int messageBoundary) throws IOException {
            if (closed) {
                throw new IOException();
            }
            try {
                if (null != is) {
                    int i = 0;
                    for (int r; 0 < (r = is.read(buffer, offset + i, length - i)); ) {
                        i += r;
                    }
                    if (i != length) {
                        throw new IOException();
                    }
                } else {
                    throw new IOException("No input.");
                }
            } catch (IOException e) {
                close(e);
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
                    for (long r; 0 < (r = is.skip(n - i)); ) {
                        i += r;
                    }
                    if (i != n) {
                        throw new IOException();
                    }
                } else {
                    throw new IOException("No input.");
                }
            } catch (IOException e) {
                close(e);
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
                close(e);
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
                close(e);
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
            wire.open().cancel(true);
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
            wire.open().cancel(true);
        }
    }

//    private static class WireSingleMessageInputStream extends WireInputStream {
//        int flags;
//
//        WireSingleMessageInputStream(Wire wire, int initialFlags) {
//            super(wire);
//            flags = initialFlags;
//        }
//
//        @Override
//        public int read(byte[] b, int off, int len) throws IOException {
//            try {
//                return wire.read(b, off, len, flags);
//            } finally {
//                flags = 0;
//            }
//        }
//    }
//
//
//    private static class WireSingleMessageOutputStream extends WireOutputStream {
//        int flags;
//
//        WireSingleMessageOutputStream(Wire wire, int initialFlags) {
//            super(wire);
//            flags = initialFlags;
//        }
//
//        @Override
//        public void write(byte[] b, int off, int len) throws IOException {
//            try {
//                wire.write(b, off, len, flags);
//            } finally {
//                flags = 0;
//            }
//        }
//    }

}
