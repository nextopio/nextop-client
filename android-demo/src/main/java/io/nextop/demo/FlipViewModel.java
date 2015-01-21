package io.nextop.demo;

import io.nextop.Id;
import io.nextop.rx.RxManaged;
import rx.Observable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlipViewModel extends RxManaged {
    // ids
    // model internally orders by frame number

    // last update id

    List<Id> frameIds = new ArrayList<Id>(8);

    Map<Id, FrameViewModel> frameVms = new HashMap<Id, FrameViewModel>(8);





    public FlipViewModel(Id flipId) {
        super(flipId);
    }


    public int size() {
        return frameIds.size();
    }


    public Id getFrameId(int index) {
        return frameIds.get(index);
    }

    public FrameViewModel getFrameVm(Id frameId) {
        return frameVms.get(frameId);
    }

}
