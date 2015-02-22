package io.nextop.v15.fragment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.*;
import io.nextop.Nextop;
import io.nextop.NextopAndroid;
import io.nextop.v15.R;
import io.nextop.v15.rx.RxFragment;
import rx.Observer;
import rx.Subscription;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

// currently "rapid capture fragment" with goal to
// support all types of images capture and integrate smoothly with nextop upload
public class ImageCaptureFragment extends RxFragment {

    boolean previewStarted = false;

    TextureView textureView;
    SurfaceTexture surfaceTexture;

    boolean record = false;


    @Nullable
    Subscription previewSubscription = null;


    // CONFIG

    int frameIntervalMs = 50;
    int frameMaxWidthPx = 320;
    int frameMaxHeightPx = 480;



    public ImageCaptureFragment() {
    }



    public void onTouchPreview() {
        // implement here
    }

    public void onFrame(Bitmap frame) {
        // implement here
    }





    public void setRecord(boolean record) {
        this.record = record;
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image_capture, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();

        textureView = (TextureView) view.findViewById(R.id.texture);

        textureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTouchPreview();
            }
        });

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            long lastPreviewCaptureNanos = 0L;

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                surfaceTexture = surface;
                startPreview();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                surfaceTexture = null;
                stopPreview();
                return true;
            }


            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                if (record) {
                    long nanos = System.nanoTime();

                    if (TimeUnit.MILLISECONDS.toNanos(frameIntervalMs) <= nanos - lastPreviewCaptureNanos) {
                        lastPreviewCaptureNanos = nanos;


                        Bitmap bitmap = textureView.getBitmap();

                        int w = bitmap.getWidth();
                        int h = bitmap.getHeight();

                        int sw = w;
                        int sh = h;
                        if (frameMaxHeightPx < h) {
                            int t = frameMaxHeightPx;
                            sw = sw * t / sh;
                            sh = t;
                        }
                        if (frameMaxWidthPx < w) {
                            int t = frameMaxWidthPx;
                            sh = sh * t / sw;
                            sw = t;
                        }

                        if (w != sw || h != sh) {
                            bitmap = Bitmap.createScaledBitmap(bitmap, sw, sh, false);
                        }

                        onFrame(bitmap);
                    }

                }
            }


            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // Do nothing
            }

        });

    }







    void startPreview() {
        previewStarted = true;
        if (null == previewSubscription || previewSubscription.isUnsubscribed()) {
            if (null != surfaceTexture) {
                previewSubscription = NextopAndroid.getActive(getActivity()).camera().subscribe(new Observer<Nextop.CameraAdapter>() {
                    @Nullable
                    Nextop.CameraAdapter ca = null;

                    @Override
                    public void onNext(Nextop.CameraAdapter ca) {
                        stop();
                        assert null == this.ca;
                        try {
                            this.ca = ca;
                            Camera camera = ca.camera;
                            camera.setPreviewTexture(surfaceTexture);
                            setCameraDisplayOrientation(getActivity(), ca.cameraId, camera);
                            camera.startPreview();
                        } catch (IOException e) {
                            //
                        }
                    }

                    @Override
                    public void onCompleted() {
                        stop();
                        restart();
                    }

                    @Override
                    public void onError(Throwable e) {
                        stop();
                        restart();
                    }

                    void stop() {
                        if (null != ca) {
                            ca.camera.stopPreview();
                            ca = null;
                        }
                    }

                    void restart() {
                        if (previewStarted) {
                            startPreview();
                        }
                    }

                });
            }
        }
    }
    void stopPreview() {
        previewStarted = false;
        if (null != previewSubscription) {
            previewSubscription.unsubscribe();
        }
    }




    @Override
    public void onResume() {
        super.onResume();

        NextopAndroid.getActive(getActivity()).addCameraUser();
        startPreview();

    }

    @Override
    public void onPause() {
        super.onPause();

        stopPreview();
        NextopAndroid.getActive(getActivity()).removeCameraUser();
    }


    /** from the Android docs */
    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, Camera camera) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
}
