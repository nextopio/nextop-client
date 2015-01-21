package io.nextop.demo;

import io.nextop.Id;
import io.nextop.rx.RxManaged;
import io.nextop.rx.RxManager;
import rx.functions.Action1;

public class FlipInfoViewModelManager extends RxManager<FlipInfoViewModel> {



    void setIntro(Id flipId, final String intro) {
        update(flipId, new Action1<ManagedState<FlipInfoViewModel>>() {
            @Override
            public void call(ManagedState<FlipInfoViewModel> state) {
                state.m.intro = intro;
            }
        });
    }




    @Override
    protected FlipInfoViewModel create(Id id) {
        return new FlipInfoViewModel(id);
    }
}
