package io.nextop;

import java.nio.ByteBuffer;

/** To convert this to pixels, do transcoding, or scaling,
 * use the Nextop* utility for the platform.
 * E.g. on Android, use <code>NextopAndroid.toBitmap(EncodedImage)</code>. */
public class EncodedImage {
    public static enum Format {
        WEBP,
        JPEG,
        PNG
    }
    public static enum Orientation {
        REAR_FACING,
        /** mirrored */
        FRONT_FACING
    }

    public static final int UNKNOWN_WIDTH = -1;
    public static final int UNKNOWN_HEIGHT = -1;


    public static EncodedImage webp(byte[] bytes) {
        return webp(UNKNOWN_WIDTH, UNKNOWN_HEIGHT, bytes);
    }
    public static EncodedImage webp(int width, int height,
                                    byte[] bytes) {
        return create(Format.WEBP, Orientation.REAR_FACING, width, height, bytes, 0, bytes.length);
    }

    public static EncodedImage jpeg(byte[] bytes) {
        return jpeg(UNKNOWN_WIDTH, UNKNOWN_HEIGHT, bytes);
    }
    public static EncodedImage jpeg(int width, int height,
                                      byte[] bytes) {
        return create(Format.JPEG, Orientation.REAR_FACING, width, height, bytes, 0, bytes.length);
    }

    public static EncodedImage png(byte[] bytes) {
        return jpeg(UNKNOWN_WIDTH, UNKNOWN_HEIGHT, bytes);
    }
    public static EncodedImage png(int width, int height,
                                    byte[] bytes) {
        return create(Format.PNG, Orientation.REAR_FACING, width, height, bytes, 0, bytes.length);
    }


    public static EncodedImage create(Format format, Orientation orientation, int width, int height,
                                      byte[] bytes, int offset, int length) {
        return new EncodedImage(format, orientation, width, height,
                bytes, offset, length);
    }


    public final Format format;
    public final Orientation orientation;
    public final int width;
    public final int height;
    protected final byte[] bytes;
    protected final int offset;
    protected final int length;


    protected EncodedImage(Format format, Orientation orientation,
                           int width, int height,
                           byte[] bytes, int offset, int length) {
        this.format = format;
        this.orientation = orientation;
        this.width = width;
        this.height = height;
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }


    protected ByteBuffer toBuffer() {
        return ByteBuffer.wrap(bytes, offset, length);
    }
}
