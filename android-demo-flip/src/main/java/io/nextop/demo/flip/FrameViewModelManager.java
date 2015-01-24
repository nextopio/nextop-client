package io.nextop.demo.flip;

import io.nextop.Id;
import io.nextop.rx.RxManager;

public class FrameViewModelManager extends RxManager<FrameViewModel> {

    @Override
    protected FrameViewModel create(Id id) {
        return new FrameViewModel(id);
    }
}
