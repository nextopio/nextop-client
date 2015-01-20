package io.nextop.demo;

import io.nextop.Id;
import io.nextop.rx.RxManaged;
import io.nextop.rx.RxManager;

public class FlipInfoViewModelManager extends RxManager<FlipInfoViewModel> {

    @Override
    protected FlipInfoViewModel create(Id id) {
        return new FlipInfoViewModel(id);
    }
}
