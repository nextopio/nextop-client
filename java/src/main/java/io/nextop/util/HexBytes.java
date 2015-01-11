package io.nextop.util;

import java.nio.CharBuffer;

public class HexBytes {

    public static byte[] valueOf(String s) {
        int n = s.length();
        byte[] bytes = new byte[n / 2];
        for (int i = 0; i < n; i += 2) {
            int a = hexToNibble[s.charAt(i)];
            if (a < 0) {
                throw new IllegalArgumentException();
            }
            int b = hexToNibble[s.charAt(i + 1)];
            if (b < 0) {
                throw new IllegalArgumentException();
            }
            bytes[i / 2] = (byte) ((a << 4) | b);
        }
        return bytes;
    }

    public static String toString(byte[] bytes) {
        int n = bytes.length;
        CharBuffer cb = CharBuffer.allocate(2 * n);
        toString(bytes, 0, n, cb);
        return new String(cb.array());
    }
    public static void toString(byte[] bytes, int offset, int n, CharBuffer cb) {
        for (int i = 0; i < n; ++i) {
            cb.put(byteToHex[0xFF & bytes[offset + i]]);
        }
    }



    /////// LUTs ///////

    private static final char[] nibbleToHex;
    private static final int[] hexToNibble;
    private static final char[][] byteToHex;
    static {
        nibbleToHex = new char[16];
        for (int i = 0; i < 16; ++i) {
            nibbleToHex[i] = Character.toLowerCase(Integer.toHexString(i).charAt(0));
        }
        hexToNibble = new int[128];
        for (int i = 0, n = hexToNibble.length; i < n; ++i) {
            hexToNibble[i] = -1;
        }
        for (int i = 0; i < 16; ++i) {
            hexToNibble[Character.toLowerCase(nibbleToHex[i])] = i;
            hexToNibble[Character.toUpperCase(nibbleToHex[i])] = i;
        }
        byteToHex = new char[256][];
        for (int i = 0; i < 256; ++i) {
            byteToHex[i] = new char[]{nibbleToHex[(i >>> 4) & 0x0F], nibbleToHex[i & 0x0F]};
        }
    }
}
