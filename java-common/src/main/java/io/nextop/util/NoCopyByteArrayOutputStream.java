package io.nextop.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NoCopyByteArrayOutputStream extends OutputStream {
    private byte[] buffer;
    private int bufferOffset;
    private int bufferHead;

    private final byte[] one = new byte[1];


    public NoCopyByteArrayOutputStream(int size) {
        this(new byte[size]);
    }
    public NoCopyByteArrayOutputStream(byte[] buffer) {
        this(buffer, 0);
    }
    public NoCopyByteArrayOutputStream(byte[] buffer, int offset) {
        if (buffer.length < offset) {
            throw new IllegalArgumentException();
        }
        this.buffer = buffer;
        bufferOffset = offset;
        bufferHead = offset;
    }


    public byte[] getBytes() {
        return buffer;
    }

    public int getOffset() {
        return bufferOffset;
    }

    public int getLength() {
        return bufferHead - bufferOffset;
    }

    public byte[] toByteArray() {
        int n = bufferHead - bufferOffset;
        byte[] copy = new byte[n];
        System.arraycopy(buffer, bufferOffset, copy, 0, n);
        return copy;
    }


    /////// OutputStream ///////

    @Override
    public void write(int b) {
        one[0] = (byte) b;
        write(one, 0, 1);
    }

    @Override
    public void write(byte[] bytes) {
        write(bytes, 0, bytes.length);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) {
        if (bytes.length < offset + length) {
            throw new IndexOutOfBoundsException();
        }
        if (buffer.length < bufferHead + length) {
            // resize the buffer to fit
            int n = buffer.length - bufferOffset;
            int i = bufferHead - bufferOffset;
            while (n < i + length) {
                n *= 2;
            }
            byte[] newBuffer = new byte[n];
            System.arraycopy(buffer, bufferOffset, newBuffer, 0, i);
            buffer = newBuffer;
            bufferOffset = 0;
            bufferHead = i;
        }

        // copy in
        System.arraycopy(bytes, offset, buffer, bufferHead, length);
        bufferHead += length;
    }

    @Override
    public void flush() {
        // Do nothing
    }

    @Override
    public void close() {
        // Do nothing
    }

}
