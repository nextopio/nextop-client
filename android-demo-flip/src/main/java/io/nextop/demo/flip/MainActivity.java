package io.nextop.demo.flip;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import io.nextop.Id;
import io.nextop.rx.RxActivity;
import io.nextop.rx.RxViewGroup;
import io.nextop.view.ImageView;
import io.nextop.vm.ImageViewModel;
import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.annotation.Nullable;

public class MainActivity extends RxActivity {

    Flip flip;
    FeedAdapter feedAdapter;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        flip = (Flip) getApplication();
        feedAdapter = new FeedAdapter();


        setContentView(R.layout.activity_main);

        GridView gridView = (GridView) findViewById(R.id.grid);
        gridView.setAdapter(feedAdapter);

        ImageButton createButton = (ImageButton) findViewById(R.id.create_button);


        bind(flip.getFeedVmm().get(flip.getFeedId())).subscribe(feedAdapter);


        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Id flipId = feedAdapter.getItem(position);

                startActivity(FlipActivity.viewIntent(MainActivity.this, flipId));
            }
        });

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Id flipId = Id.create();

                // pop up a dialog asking for title


                // FIXME speech recognize the title; don't prompt for it
//                FlipViewModel flipVm = new FlipViewModel(flipId);
//                flipVm.title = "";





                flip.getFeedVmm().addFlip(flip.getFeedId(), flipId);





                startActivity(FlipActivity.recordIntent(MainActivity.this, flipId));

            }
        });

    }

//
//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        // FIXME this should happen via bind(...) subscribes when bind works!
//        feedAdapter.notifyDataSetChanged();
//    }

    private class FeedAdapter extends BaseAdapter implements Observer<FeedViewModel> {
        @Nullable
        FeedViewModel feedVm = null;


        @Override
        public void onNext(FeedViewModel feedVm) {
            this.feedVm = feedVm;
            notifyDataSetChanged();
        }

        @Override
        public void onCompleted() {
            feedVm = null;
            notifyDataSetChanged();
        }

        @Override
        public void onError(Throwable e) {
            feedVm = null;
            notifyDataSetChanged();
        }



        @Override
        public int getCount() {
            return null != feedVm ? feedVm.size() : 0;
        }

        @Override
        public Id getItem(int position) {
            return feedVm.getFlipId(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).longHashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // FIXME demo hack - because rxVg.reset() doesn't work
            if (null == convertView) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_feed_tile, parent, false);
                @Nullable RxViewGroup rxVg = (RxViewGroup) convertView.findViewWithTag(RxViewGroup.TAG);
                if (null != rxVg) {
                    rxVg.cascadeDispose(MainActivity.this);
                }
            }

            Id flipId = getItem(position);

            @Nullable RxViewGroup rxVg = (RxViewGroup) convertView.findViewWithTag(RxViewGroup.TAG);
            if (null != rxVg) {
                rxVg.reset();
                updateIndefinitely(convertView, rxVg.bind(flip.getFlipInfoVmm().get(flipId)), rxVg.bind(flip.getFlipVmm().get(flipId)));
            } else {
                // update immediate snapshot
                updateIndefinitely(convertView, flip.getFlipInfoVmm().peek(flipId), flip.getFlipVmm().peek(flipId));
            }

            return convertView;
        }
        // assume the subscription will be managed via onComplete from a higher level
        // this code just assumes it runs until done
        private void updateIndefinitely(View view, Observable<FlipInfoViewModel> flipInfoVmSource, Observable<FlipViewModel> flipVmSource) {
            ImageView imageView = (ImageView) view.findViewById(R.id.image);
            final TextView shortIntroView = (TextView) view.findViewById(R.id.short_intro);

            // FIXME split out the image source into its own observable

//            final Subscription[] HACK = new Subscription[1];
            Observable<ImageViewModel> imageVmSource = flipVmSource

                    // FIXME holy shit, massive demo hack because upload progress is not reactive
//                    .take(1).repeat(AndroidSchedulers.mainThread()).delay(100, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())

                    .flatMap(new Func1<FlipViewModel, Observable<ImageViewModel>>() {
                        @Override
                        public Observable<ImageViewModel> call(FlipViewModel flipVm) {
                            int n = flipVm.size();
                            if (0 < n) {
                                // find the first with remaining upload progress
                                for (int i = 0; i < n; ++i) {
                                    FrameViewModel frameVm = flipVm.getFrameVm(flipVm.getFrameId(i));
                                    if (frameVm.imageVm.uploadProgress < 1.f) {
                                        return Observable.just(frameVm.imageVm);
                                    }
                                }
                                // else return the first
                                return Observable.just(flipVm.getFrameVm(flipVm.getFrameId(0)).imageVm);
                            } else {
//                                AndroidSchedulers.mainThread().createWorker().schedule(new Action0() {
//                                                                                           @Override
//                                                                                           public void call() {
//                                                                                               HACK[0].unsubscribe();
//                                                                                           }
//                                                                                       });


                                return Observable.empty();
                            }
                        }
                    })


            .distinctUntilChanged();
            /*HACK[0] =*/
            imageVmSource.subscribe(new ImageView.Updater(imageView));




            Observable<String> shortIntroSource = flipInfoVmSource.map(new Func1<FlipInfoViewModel, String>() {
                @Override
                public String call(FlipInfoViewModel flipInfoVm) {
                    return flipInfoVm.intro;
                }
            });
            shortIntroSource.subscribe(new Action1<String>() {
                @Override
                public void call(String intro) {
                    shortIntroView.setText(intro);
                }
            });
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }
    }
}
