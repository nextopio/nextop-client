package io.nextop.demo;

import io.nextop.Id;
import io.nextop.rx.RxManaged;
import io.nextop.rx.RxManager;

public class FeedViewModelManager extends RxManager<FeedViewModel> {

    @Override
    protected FeedViewModel create(Id id) {
        return new FeedViewModel(id);
    }


}
