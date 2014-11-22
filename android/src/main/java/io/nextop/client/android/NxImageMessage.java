package io.nextop.client.android;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

public class NxImageMessage extends NxMessage {
    public static enum CameraSource {
        NONE,
        FRONT_FACING,
        REAR_FACING
    }


    CameraSource cameraSource;
    // bodyBytes is the raw JPEG bytes
    @Nullable Bitmap bitmap;
}
