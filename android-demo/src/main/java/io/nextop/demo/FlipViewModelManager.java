package io.nextop.demo;

import android.graphics.Bitmap;
import io.nextop.Id;
import io.nextop.rx.RxManaged;
import io.nextop.rx.RxManager;
import rx.functions.Action1;

public class FlipViewModelManager extends RxManager<FlipViewModel> {


    void addFrame(Id flipId, final Id frameId) {
        update(flipId, new Action1<ManagedState<FlipViewModel>>() {
            @Override
            public void call(ManagedState<FlipViewModel> state) {
                state.m.ids.add(frameId);
            }
        });
    }


    @Override
    protected FlipViewModel create(Id id) {
        return new FlipViewModel(id);
    }
}
