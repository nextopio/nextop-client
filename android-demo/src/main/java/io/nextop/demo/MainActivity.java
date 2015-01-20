package io.nextop.demo;


import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import io.nextop.Id;
import io.nextop.rx.RxActivity;
import io.nextop.rx.RxManager;
import io.nextop.rx.RxViewGroup;
import io.nextop.view.ImageView;
import io.nextop.vm.ImageViewModel;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.internal.util.SubscriptionList;

import javax.annotation.Nullable;

public class MainActivity extends RxActivity {

    Demo demo;
    FeedAdapter feedAdapter;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        demo = (Demo) getApplication();
        feedAdapter = new FeedAdapter();


        setContentView(R.layout.activity_main);

        GridView gridView = (GridView) findViewById(R.id.grid);
        gridView.setAdapter(feedAdapter);

        Button createButton = (Button) findViewById(R.id.create_button);


        bind(demo.getFeedVmm().get(demo.getFeedId())).subscribe(feedAdapter);


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


                // FIXME
//                demo.getFlipVmm().create(flipId, flipVm);

                Toast toast = Toast.makeText(MainActivity.this, "Speak an intro now ...", Toast.LENGTH_LONG);
                toast.show();

                startActivity(FlipActivity.recordIntent(MainActivity.this, flipId));

            }
        });

    }


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
            }

            Id flipId = getItem(position);

            final RxViewGroup rxVg = (RxViewGroup) convertView.findViewWithTag(RxViewGroup.TAG);
            if (null != rxVg) {
                rxVg.reset();
                updateIndefinitely(convertView, rxVg.bind(demo.getFlipInfoVmm().get(flipId)));
            } else {
                // update immediate snapshot
                updateIndefinitely(convertView, demo.getFlipInfoVmm().peek(flipId));
            }

            return convertView;
        }
        // assume the subscription will be managed via onComplete from a higher level
        // this code just assumes it runs until done
        private void updateIndefinitely(View view, Observable<FlipInfoViewModel> flipInfoVmSource) {
            ImageView imageView = (ImageView) view.findViewById(R.id.image);
            final TextView shortIntroView = (TextView) view.findViewById(R.id.short_intro);


            Observable<ImageViewModel> imageVmSource = flipInfoVmSource.map(new Func1<FlipInfoViewModel, ImageViewModel>() {
                @Override
                public ImageViewModel call(FlipInfoViewModel flipInfoVm) {
                    return flipInfoVm.imageVm;
                }
            }).distinctUntilChanged();
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
