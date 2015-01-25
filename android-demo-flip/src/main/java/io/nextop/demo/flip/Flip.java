package io.nextop.demo.flip;

import android.hardware.Camera;
import android.support.annotation.Nullable;
import io.nextop.Id;
import io.nextop.NextopApplication;

import java.io.IOException;

public class Flip extends NextopApplication {
    private final Id accountId = Id.create();

    private FeedViewModelManager feedVmm = new FeedViewModelManager();
    private FlipInfoViewModelManager flipInfoVmm = new FlipInfoViewModelManager();
    private FlipViewModelManager flipVmm = new FlipViewModelManager();





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



}
