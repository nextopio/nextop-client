package io.nextop;

import io.nextop.util.HexBytes;

import java.net.InetAddress;
import java.nio.CharBuffer;
import java.util.Arrays;

/** limited version of {@link java.net.InetAddress} that
 * only does IP (no DNS lookups) */
public abstract class Ip {
    public static Ip valueOf(String s) {
        for (int i = 0, n = s.length(); i < n; ++i) {
            switch (s.charAt(i)) {
                case '.':
                    return V4.valueOf(s);
                case ':':
                    return V6.valueOfV6(s);
            }
        }
        throw new IllegalArgumentException();
    }

    public static Ip valueOf(InetAddress address) {
        return create(address.getAddress());
    }

    public static Ip create(byte[] address) {
        switch (address.length) {
            case 4:
                return V4.create(address);
            case 16:
                return V6.create(address);
            default:
                throw new IllegalArgumentException();
        }
    }


    protected final byte[] bytes;
    private final int hashCode;

    protected Ip(byte[] bytes) {
        this.bytes = bytes;
        hashCode = Arrays.hashCode(bytes);
    }


    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Ip)) {
            return false;
        }
        Ip b = (Ip) obj;
        return hashCode == b.hashCode && Arrays.equals(bytes, b.bytes);
    }



    public static class V4 extends Ip {

        public static Ip valueOf(String s) {
            byte[] bytes = new byte[4];
            int i = 0;
            int j = 0;
            int n = s.length();
            int m = 0;
            for (; m < 4 && j <= n; ++j) {
                if (n == j || '.' == s.charAt(j)) {
                    if (i == j) {
                        throw new IllegalArgumentException();
                    }
                    int b = Integer.parseInt(s.substring(i, j));
                    if (b < 0 || 255 < b) {
                        throw new IllegalArgumentException();
                    }
                    bytes[m] = (byte) b;
                    ++m;
                    i = j + 1;
                }
            }
            if (4 != m) {
                throw new IllegalArgumentException();
            }
            return new V4(bytes);
        }

        public static V4 create(byte[] bytes) {
            if (4 != bytes.length) {
                throw new IllegalArgumentException();
            }
            return new V4(bytes);
        }


        private V4(byte[] bytes) {
            super(bytes);
        }

        @Override
        public String toString() {
            return String.format("%d.%d.%d.%d",
                    0xFF & bytes[0],
                    0xFF & bytes[1],
                    0xFF & bytes[2],
                    0xFF & bytes[3]);
        }
    }

    public static class V6 extends Ip {

        public static Ip valueOfV6(String s) {
            if (32 + 7 != s.length()) {
                throw new IllegalArgumentException();
            }

            byte[] bytes = new byte[16];
            int i = 0;
            int j = 0;
            int n = s.length();
            int m = 0;
            for (; m < 16 && j <= n; ++j) {
                if (n == j || ':' == s.charAt(j)) {
                    if (i == j) {
                        // an empty block at the end is shorthand for remaining 0s
                        if (n - 1 == j) {
                            while (m < 16) {
                                bytes[m] = 0;
                                ++m;
                            }
                        } else {
                            throw new IllegalArgumentException();
                        }
                    } else {
                        byte[] b2 = HexBytes.valueOf(s.substring(i, j));
                        if (2 != b2.length) {
                            throw new IllegalArgumentException();
                        }
                        bytes[m] = b2[0];
                        bytes[m + 1] = b2[1];
                        m += 2;
                        i = j + 1;
                    }
                }
            }
            if (16 != m) {
                throw new IllegalArgumentException();
            }
            return new V6(bytes);
        }

        public static V6 create(byte[] bytes) {
            if (16 != bytes.length) {
                throw new IllegalArgumentException();
            }
            return new V6(bytes);
        }


        private V6(byte[] bytes) {
            super(bytes);
        }

        @Override
        public String toString() {
            CharBuffer cb = CharBuffer.allocate(32 + 7);
            for (int i = 0; i < 8; ++i) {
                if (0 < i) {
                    cb.put(':');
                }
                HexBytes.toString(bytes, 2 * i, 2, cb);
            }
            return new String(cb.array());
        }
    }
}
