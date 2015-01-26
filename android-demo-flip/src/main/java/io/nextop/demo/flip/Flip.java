package io.nextop.demo.flip;

import io.nextop.Id;
import io.nextop.NextopApplication;

public class Flip extends NextopApplication {
    private final Id feedId = Id.valueOf("0000000000000000000000000000000000000000000000000000000000000000");

    private FeedViewModelManager feedVmm = new FeedViewModelManager();
    private FlipInfoViewModelManager flipInfoVmm = new FlipInfoViewModelManager();
    private FlipViewModelManager flipVmm = new FlipViewModelManager();


    public Id getFeedId() {
        // at this point everyone shares the same feed
        return feedId;
    }


    public FeedViewModelManager getFeedVmm() {
        return feedVmm;
    }

    public FlipInfoViewModelManager getFlipInfoVmm() {
        return flipInfoVmm;
    }

    public FlipViewModelManager getFlipVmm() {
        return flipVmm;
    }



}
