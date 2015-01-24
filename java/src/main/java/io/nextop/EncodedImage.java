package io.nextop;

import java.nio.ByteBuffer;

public class EncodedImage {
    static enum Format {
        WEBP,
        JPEG,
        PNG
    }
    static enum Orientation {
        REAR_FACING,
        /** mirrored */
        FRONT_FACING
    }



    Format format;
    byte[] bytes;
    int offset;
    int length;




}
