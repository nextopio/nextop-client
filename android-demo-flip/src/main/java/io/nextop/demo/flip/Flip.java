package io.nextop.demo.flip;

import android.hardware.Camera;
import android.support.annotation.Nullable;
import io.nextop.Id;
import io.nextop.NextopApplication;

import java.io.IOException;

public class Flip extends NextopApplication {
    private final Id accountId = Id.create();

    private FeedViewModelManager feedVmm = new FeedViewModelManager();
    private FlipInfoViewModelManager flipInfoVmm = new FlipInfoViewModelManager();
    private FlipViewModelManager flipVmm = new FlipViewModelManager();



    int cameraId = -1;
    @Nullable
    Camera camera = null;
    boolean cameraConnected = false;

    void lockCamera() {
        openCamera();
        try {
            if (!cameraConnected) {
                if (cameraId < 0) {
                    cameraId = getDefaultCameraId();
                }
                if (null == camera) {
                    if (0 <= cameraId) {
                        try {
                            camera = Camera.open(cameraId);
                            if (null != camera) {
                                cameraConnected = true;
                            }
                        } catch (Exception e) {
                            // e.g. Fail to connect to camera service
                        }
                    }
                } else {
                    camera.reconnect();
                    cameraConnected = true;
                }
            }
        } catch (IOException e) {
            //
        }
    }
    void unlockCamera() {
        if (cameraConnected) {
//            camera.release();
            cameraConnected = false;
            camera.unlock();
        }
    }


    void openCamera() {
        if (null == camera) {
            try {
                camera = Camera.open();
            } catch (Exception e) {
                // e.g. Fail to connect to camera service

            }
        }
    }

    void closeCamera() {
        unlockCamera();
        if (null != camera) {
            cameraId = -1;
            camera.release();
            camera = null;
        }
    }



    static int getDefaultCameraId() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return 1 <= numberOfCameras ? 0 : -1;
    }



    // FIXME model, controllers


    public Id getFeedId() {
        // at this point everyone shares the same feed
        return accountId;
    }


    public FeedViewModelManager getFeedVmm() {
        return feedVmm;
    }

    public FlipInfoViewModelManager getFlipInfoVmm() {
        return flipInfoVmm;
    }

    public FlipViewModelManager getFlipVmm() {
        return flipVmm;
    }



}
