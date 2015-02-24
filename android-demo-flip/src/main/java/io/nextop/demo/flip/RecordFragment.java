package io.nextop.demo.flip;

import android.graphics.Bitmap;
import android.os.Bundle;
import io.nextop.Id;
import io.nextop.v15.fragment.ImageCaptureFragment;
import io.nextop.vm.ImageViewModel;

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
