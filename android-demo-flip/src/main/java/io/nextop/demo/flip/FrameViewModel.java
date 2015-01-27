package io.nextop.demo.flip;

import io.nextop.Id;
import io.nextop.rx.RxManaged;
import io.nextop.vm.ImageViewModel;

public class FrameViewModel extends RxManaged {
    // FIXME
    ImageViewModel imageVm = new ImageViewModel(null, null, null);
    // ImageViewModel
    // frame id
    // snap time
    // last update id
    long creationTime = 0L;


    public FrameViewModel(Id frameId) {
        super(frameId);
    }

}
