package io.nextop.demo.flip;

import io.nextop.Id;
import io.nextop.rx.RxManager;
import rx.functions.Action1;

public class FlipViewModelManager extends RxManager<FlipViewModel> {

    // CONTROLLER

    void addFrame(Id flipId, final FrameViewModel frameVm) {
        update(flipId, new Action1<ManagedState<FlipViewModel>>() {
            @Override
            public void call(ManagedState<FlipViewModel> state) {
                state.m.frameVms.put(frameVm.id, frameVm);
                state.m.frameIds.add(frameVm.id);
            }
        });


        // FIXME update
        // FIXME send the image to the endpoint
        // FIXME replace the imageVm with a local id
        // FIXME
    }

    
    // VMM

    @Override
    protected void startUpdates(ManagedState<FlipViewModel> state) {
        // sync
        // get updates. updates are (frame id, creation time)
        // in flipvm sort by creation time to get the final ordering
    }

    @Override
    protected FlipViewModel create(Id id) {
        return new FlipViewModel(id);
    }
}
