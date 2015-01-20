package io.nextop.demo;

import io.nextop.Id;
import io.nextop.rx.RxManaged;

import java.util.ArrayList;
import java.util.List;

public class FeedViewModel extends RxManaged {
    // ids
    // model internally orders by last modified time

    // last update id

    // on change observer


    List<Id> fakeIds = new ArrayList<Id>(50);
    {
        for (int i = 0; i < 50; ++i) {
            fakeIds.add(Id.create());
        }
    }


    public FeedViewModel(Id feedId) {
        super(feedId);
    }


    public int size() {
        return fakeIds.size();
    }


    public Id getFlipId(int index) {
        return fakeIds.get(index);
    }



}
