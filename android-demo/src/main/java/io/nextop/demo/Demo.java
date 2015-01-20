package io.nextop.demo;

import io.nextop.Id;
import io.nextop.NextopApplication;

public class Demo extends NextopApplication {
    private final Id accountId = Id.create();

    private FeedViewModelManager feedVmm = new FeedViewModelManager();
    private FlipInfoViewModelManager flipInfoVmm = new FlipInfoViewModelManager();
    private FlipViewModelManager flipVmm = new FlipViewModelManager();
    private FrameViewModelManager frameVmm = new FrameViewModelManager();


    // FIXME model, controllers


    public Id getFeedId() {
        // at this point everyone shares the same feed
        return accountId;
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

    public FrameViewModelManager getFrameVmm() {
        return frameVmm;
    }


}
