package io.nextop.demo.flip;

import io.nextop.Id;
import io.nextop.NextopApplication;

public class Flip extends NextopApplication {
    static final String REMOTE = "localhost:3770";
            //"demo-flip.nextop.io";

    /** @see backend-demo-flip index.js */
    private final Id feedId = Id.valueOf("0000000000000000000000000000000000000000000000000000000000000000");

    private FeedViewModelManager feedVmm;
    private FlipInfoViewModelManager flipInfoVmm;
    private FlipViewModelManager flipVmm;


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



    @Override
    public void onCreate() {
        super.onCreate();

        feedVmm = new FeedViewModelManager(getNextop());
        flipInfoVmm = new FlipInfoViewModelManager(getNextop());
        flipVmm = new FlipViewModelManager(getNextop());
    }


}
