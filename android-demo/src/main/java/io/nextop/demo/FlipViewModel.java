package io.nextop.demo;

import io.nextop.Id;
import io.nextop.rx.RxManaged;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;

public class FlipViewModel extends RxManaged {
    // ids
    // model internally orders by frame number

    // last update id

    List<Id> fakeIds = new ArrayList<Id>(50);
    {
        for (int i = 0; i < 50; ++i) {
            fakeIds.add(Id.create());
        }
    }



    public FlipViewModel(Id flipId) {
        super(flipId);
    }


    public int size() {
        return fakeIds.size();
    }


    public Id getFrameId(int index) {
        return fakeIds.get(index);
    }
}
