package io.nextop.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.*;
import io.nextop.Id;
import io.nextop.rx.RxFragment;
import io.nextop.vm.ImageViewModel;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RecordFragment extends RxFragment {
    public static RecordFragment newInstance(Id flipId) {
        Bundle args = new Bundle();
        args.putString("flipId", flipId.toString());

        RecordFragment f = new RecordFragment();
        f.setArguments(args);
        return f;
    }



    Id flipId;
    Demo demo;


    boolean previewStarted = false;

    TextureView textureView;
    SurfaceTexture surfaceTexture;

    boolean record = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        flipId = Id.valueOf(getArguments().getString("flipId"));
        demo = (Demo) getActivity().getApplication();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // FIXME set up camera

        View view = getView();

        textureView = (TextureView) view.findViewById(R.id.texture);

        textureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FlipActivity) getActivity()).stopRecording();
            }
        });

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
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


            long lastCaptureNanos = 0L;
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                // FIXME if record, push a frame here
                if (record) {
                    long nanos = System.nanoTime();

                    if (TimeUnit.MILLISECONDS.toNanos(200) <= nanos - lastCaptureNanos) {
                        lastCaptureNanos = nanos;


                        Bitmap bitmap = textureView.getBitmap();

                        int sw = 320;
                        int sh = bitmap.getHeight() * sw / bitmap.getWidth();
                        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, sw, sh, false);


                        Id frameId = Id.create();
                        FrameViewModel frameVm = new FrameViewModel(frameId);
                        frameVm.imageVm = ImageViewModel.memory(scaled);
                        demo.getFlipVmm().addFrame(flipId, frameVm);
                    }

                }
            }


            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // Do nothing
            }

        });

    }


    void onStartRecording() {
        // TODO set preview buffer, attach preview callback

//        demo.camera.addCallbackBuffer(new byte[1024 * 1204]);
//        demo.camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
//            @Override
//            public void onPreviewFrame(byte[] data, Camera camera) {
//
//            }
//        });

        record = true;
    }
    void onStopRecording() {
        // TODO unattach preview callback, clear buffer

        record = false;
    }



    void startPreview() {
        if (!previewStarted) {
            // TODO retry this if camera not opened
            demo.lockCamera();
            if (demo.cameraConnected && null != surfaceTexture) {
                try {
                    demo.camera.setPreviewTexture(surfaceTexture);
                    setCameraDisplayOrientation(getActivity(), demo.cameraId, demo.camera);
                    demo.camera.startPreview();
                    previewStarted = true;
                } catch (IOException e) {
                    //
                }
            }
        }
    }
    void stopPreview() {
        if (previewStarted) {
            previewStarted = false;
            if (null != demo.camera) {
                demo.camera.stopPreview();
            }
        }
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
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



    @Override
    public void onResume() {
        super.onResume();

        startPreview();

    }

    @Override
    public void onPause() {
        super.onPause();

        stopPreview();
        demo.closeCamera();
    }
}
