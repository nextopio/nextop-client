package io.nextop;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
        /** this is the standard orientation. Any image where the orientation is unknown should use this. */
        REAR_FACING,
        /** mirrored */
        FRONT_FACING
    }

    public static final Orientation DEFAULT_ORIENTATION = Orientation.REAR_FACING;


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

    public InputStream getInputStream() {
        return new ByteArrayInputStream(bytes, offset, length);
    }


    @Override
    public int hashCode() {
        int c = format.hashCode();
        c = 31 * c + orientation.hashCode();
        c = 31 * c + width;
        c = 31 * c + height;
        c = 31 * c + length;
        for (int i = 0; i < length; ++i) {
            c = 31 * c + bytes[offset + i];
        }
        return c;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EncodedImage)) {
            return false;
        }

        EncodedImage b = (EncodedImage) obj;
        if (!(format.equals(b.format)
                && orientation.equals(b.orientation)
                && width == b.width
                && height == b.height
                && length == b.length)) {
            return false;
        }

        for (int i = 0; i < length; ++i) {
            if (bytes[offset + i] != b.bytes[b.offset + i]) {
                return false;
            }
        }

        return true;
    }
}
