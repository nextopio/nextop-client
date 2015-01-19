package io.nextop.demo;

import io.nextop.Id;
import io.nextop.ImageViewModel;
import io.nextop.NextopApplication;
import rx.Observable;

public class Demo extends NextopApplication {

    // FIXME model, controllers



    public FeedModel getFeedModel() {

    }

    public FlipInfoModel getFlipInfo(Id id) {

    }

    public FlipModel getFlipModel(Id id) {


    }

    public FlipFrame getFlipFrame(Id id, Id frameId) {


    }






    public static class FeedModel {
        // ids
        // model internally orders by last modified time

        // last update id

        // on change observer

        public Id get(int index) {

        }

        public int size() {

        }


        public Observable<FeedModel> getChangeObservable() {

        }

    }

    public static class FlipInfoModel {
        // id, name, top image uri,
        // last modified time
        // last update id
        // boolean uploading
    }

    public static class FlipModel {
        // ids
        // model internally orders by frame number

        // last update id

        public int size() {

        }


        public Observable<FlipModel> getChangeObservable() {

        }
    }

    public static class FlipFrame extends ImageViewModel {
        // frame id
        // snap time
        // last update id
    }
}
