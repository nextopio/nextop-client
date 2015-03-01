package io.nextop;

import com.google.common.hash.Hashing;
import io.nextop.util.HexBytes;

import java.lang.IllegalArgumentException;import java.lang.Object;import java.lang.Override;import java.lang.String;import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

/** 256-bit UUID.
 * @see <a href="http://www.ietf.org/rfc/rfc4122.txt">RFC4122</a>
 * @see #create */
public final class Id implements Comparable<Id> {
    private static final SecureRandom sr = new SecureRandom();

    static final int LENGTH = 32;

    /** generate a 256-bit UUID as a 128-bit UUID + 128 bits of randomness
     * @see <a href="http://www.ietf.org/rfc/rfc4122.txt">RFC4122</a>
     * @see java.util.UUID */
    public static Id create() {
        UUID uuid16 = UUID.randomUUID();
        byte[] rand16 = new byte[16];
        sr.nextBytes(rand16);

        return new Id(ByteBuffer.allocate(LENGTH
        ).putLong(uuid16.getMostSignificantBits()
        ).putLong(uuid16.getLeastSignificantBits()
        ).put(rand16
        ).array());
    }
    public static Id create(long a, long b, long c, long d) {
        return new Id(ByteBuffer.allocate(LENGTH
        ).putLong(a
        ).putLong(b
        ).putLong(c
        ).putLong(d
        ).array());
    }

    public static Id valueOf(String s) throws IllegalArgumentException {
        if (64 != s.length()) {
            throw new IllegalArgumentException();
        }
        byte[] bytes = HexBytes.valueOf(s);
        if (32 != bytes.length) {
            throw new IllegalArgumentException();
        }

        Id id = new Id(bytes);
        assert id.toString().equals(s);
        return id;
    }



    public static void toBytes(Id id, byte[] buffer, int offset) {
        if (offset < 0 || buffer.length <= offset + 32) {
            throw new IllegalArgumentException();
        }
        System.arraycopy(id.bytes, id.offset, buffer, offset, 32);
    }

    public static Id fromBytes(byte[] buffer, int offset) {
        if (offset < 0 || buffer.length <= offset + 32) {
            throw new IllegalArgumentException();
        }
        byte[] bytes = Arrays.copyOfRange(buffer, offset, offset + 32);
        return new Id(bytes, 0);
    }



    final byte[] bytes;
    final int offset;
    final long hashCode;


    Id(byte[] bytes) {
        this(bytes, 0);
    }
    Id(byte[] bytes, int offset) {
        this.bytes = bytes;
        this.offset = offset;
        hashCode = Hashing.murmur3_128().hashBytes(bytes, offset, LENGTH).asLong();
    }






    @Override
    public String toString() {
        return HexBytes.toString(bytes, offset, LENGTH);
    }

    @Override
    public int hashCode() {
        return (int) hashCode;
    }

    public long longHashCode() {
        return hashCode;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Id)) {
            return false;
        }
        Id b = (Id) obj;
        return hashCode == b.hashCode && Arrays.equals(bytes, b.bytes);
    }


    @Override
    public int compareTo(Id b) {
        for (int i = 0; i < 32; ++i) {
            int d = (0xFF & bytes[offset + i]) - (0xFF & b.bytes[b.offset + i]);
            if (0 != d) {
                return d;
            }
        }
        return 0;
    }
}
