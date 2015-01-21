package io.nextop.demo;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.*;
import io.nextop.Id;
import io.nextop.rx.RxFragment;

import java.io.IOException;

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

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;


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

        surfaceView = (SurfaceView) view.findViewById(R.id.surface);

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FlipActivity) getActivity()).stopRecording();
            }
        });

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surfaceHolder = holder;
                startPreview();
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surfaceHolder = null;
                stopPreview();
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }
        });

    }


    void onStartRecording() {
        // TODO set preview buffer, attach preview callback
    }
    void onStopRecording() {
        // TODO unattach preview callback, clear buffer
    }



    void startPreview() {
        if (!previewStarted) {
            // TODO retry this if camera not opened
            demo.lockCamera();
            if (demo.cameraConnected && null != surfaceHolder) {
                try {
                    demo.camera.setPreviewDisplay(surfaceHolder);
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
        demo.unlockCamera();
    }
}
