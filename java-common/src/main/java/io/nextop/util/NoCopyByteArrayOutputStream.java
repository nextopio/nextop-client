package io.nextop.util;

import java.io.ByteArrayOutputStream;

public class NoCopyByteArrayOutputStream extends ByteArrayOutputStream {

    public NoCopyByteArrayOutputStream(int size) {
        super(size);
    }
    public NoCopyByteArrayOutputStream(byte[] buf) {
        super(0);
        this.buf = buf;
    }


    public byte[] getBytes() {
        return buf;
    }

    public int getOffset() {
        return 0;
    }

    public int getLength() {
        return count;
    }

}
