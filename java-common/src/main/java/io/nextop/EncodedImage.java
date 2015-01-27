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
}
