package io.nextop.demo;

import io.nextop.Id;
import io.nextop.rx.RxManaged;
import io.nextop.rx.RxManager;

public class FlipViewModelManager extends RxManager<FlipViewModel> {

    @Override
    protected FlipViewModel create(Id id) {
        return new FlipViewModel(id);
    }
}
