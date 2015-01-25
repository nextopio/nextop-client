package io.nextop.demo.flip;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.*;
import io.nextop.Id;
import io.nextop.fragment.ImageCaptureFragment;
import io.nextop.rx.RxFragment;
import io.nextop.vm.ImageViewModel;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RecordFragment extends ImageCaptureFragment {
    public static RecordFragment newInstance(Id flipId) {
        Bundle args = new Bundle();
        args.putString("flipId", flipId.toString());

        RecordFragment f = new RecordFragment();
        f.setArguments(args);
        return f;
    }



    Id flipId;
    Flip flip;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        flipId = Id.valueOf(getArguments().getString("flipId"));
        flip = (Flip) getActivity().getApplication();

    }


    @Override
    public void onFrame(Bitmap frame) {

        Id frameId = Id.create();
        FrameViewModel frameVm = new FrameViewModel(frameId);
        frameVm.imageVm = ImageViewModel.memory(frame);
        flip.getFlipVmm().addFrame(flipId, frameVm);
    }


    @Override
    public void onTouchPreview() {
        ((FlipActivity) getActivity()).stopRecording();
    }

    public void onStartRecording() {
        setRecord(true);
    }

    public void onStopRecording() {
        setRecord(false);
    }
}
