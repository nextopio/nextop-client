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

    List<Id> ids = new ArrayList<Id>(50);



    public FlipViewModel(Id flipId) {
        super(flipId);
    }


    public int size() {
        return ids.size();
    }


    public Id getFrameId(int index) {
        return ids.get(index);
    }
}
