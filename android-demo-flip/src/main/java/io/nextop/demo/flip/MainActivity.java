package io.nextop.demo.flip;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import io.nextop.Id;
import io.nextop.Nextop;
import io.nextop.NextopAndroid;
import io.nextop.rx.RxActivity;
import io.nextop.rx.RxViewGroup;
import io.nextop.view.ImageView;
import io.nextop.vm.ImageViewModel;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.internal.util.SubscriptionList;
import rx.subjects.PublishSubject;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;

import javax.annotation.Nullable;
import java.util.List;

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
                if (rxVg.reset(flipId)) {
                    updateIndefinitely(convertView, rxVg.bind(flip.getFlipInfoVmm().get(flipId)), rxVg.bind(flip.getFlipVmm().get(flipId)));
                }
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

            imageView.reset();

            Observable<ImageViewModel> imageVmSource = new LoadingImageVmSource(NextopAndroid.getActive(view), flipVmSource).out
            .distinctUntilChanged();
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


    // FIXME bug where tile grid is waffling on scroll

    // TODO
    static final class LoadingImageVmSource {
        // flipVmSource subscribe X
        // X onNext (
        //    create observable for imagevm for frames
        //    unsubscribe subject from old observable
        //    subscribe subject to new observable
        // )


        Nextop nextop;
        PublishSubject<ImageViewModel> subject;

        Subscription flipVmSubscription = null;
        final Observable<ImageViewModel> out;
        Subscription emitSubscription;



        LoadingImageVmSource(Nextop nextop, final Observable<FlipViewModel> flipVmSource) {
            this.nextop = nextop;

            subject = PublishSubject.create();

            out = subject.doOnSubscribe(new Action0() {
                @Override
                public void call() {
                    flipVmSubscription = flipVmSource.doOnNext(new Action1<FlipViewModel>() {
                        @Override
                        public void call(FlipViewModel flipVm) {

                            if (null != emitSubscription) {
                                emitSubscription.unsubscribe();
                                emitSubscription = null;
                            }

                            int n = flipVm.size();
                            FrameViewModel[] frameVms = new FrameViewModel[n];
                            for (int i = 0; i < n; ++i) {
                                frameVms[i] = flipVm.getFrameVm(i);
                            }


                            CompositeSubscription s = new CompositeSubscription();
                            emitSubscription = s;

                            emit(frameVms, 0, s);





                        }
                    }).subscribe();
                }
            }).doOnUnsubscribe(new Action0() {
                @Override
                public void call() {
                    if (null != emitSubscription) {
                        emitSubscription.unsubscribe();
                        emitSubscription = null;
                    }
                    flipVmSubscription.unsubscribe();
                }
            }).share();

        }

        void emit(final FrameViewModel[] frameVms, final int i, final CompositeSubscription s) {
            if (s.isUnsubscribed()) {
                return;
            }
            if (i < frameVms.length) {
                final FrameViewModel frameVm = frameVms[i];
                if (null != frameVm.imageVm.localId) {
                    s.clear();
                    s.add(nextop.transferStatus(frameVm.id).subscribe(new Observer<Nextop.TransferStatus>() {
                        int count = 0;

                        @Override
                        public void onNext(Nextop.TransferStatus transferStatus) {
                            if (1 == ++count) {
                                if (transferStatus.progress < 1.f) {
                                    subject.onNext(frameVm.imageVm);
                                }
                            }
                        }


                        @Override
                        public void onCompleted() {
                            emit(frameVms, i + 1, s);
                        }

                        @Override
                        public void onError(Throwable e) {

                        }


                    }));

                } else {
                    emit(frameVms, i + 1, s);
                }
            } else if (0 < frameVms.length) {
                subject.onNext(frameVms[0].imageVm);
            }
        }


    }
}
