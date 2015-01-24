package io.nextop.demo.flip;

import io.nextop.Id;
import io.nextop.rx.RxManager;
import rx.functions.Action1;

public class FeedViewModelManager extends RxManager<FeedViewModel> {



    void addFlip(Id feedId, final Id flipId) {
        update(feedId, new Action1<ManagedState<FeedViewModel>>() {
            @Override
            public void call(ManagedState<FeedViewModel> state) {
                state.m.flipIds.add(0, flipId);
            }
        });
    }



    @Override
    protected FeedViewModel create(Id id) {
        return new FeedViewModel(id);
    }


}
