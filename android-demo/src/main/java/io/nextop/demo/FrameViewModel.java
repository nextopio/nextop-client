package io.nextop.demo;

import io.nextop.Id;
import io.nextop.rx.RxManaged;
import io.nextop.vm.ImageViewModel;

public class FrameViewModel extends RxManaged {
    ImageViewModel imageVm;
    // ImageViewModel
    // frame id
    // snap time
    // last update id


    public FrameViewModel(Id frameId) {
        super(frameId);
    }

}
