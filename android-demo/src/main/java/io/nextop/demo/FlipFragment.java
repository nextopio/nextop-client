package io.nextop.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import io.nextop.Id;
import io.nextop.rx.RxFragment;
import io.nextop.rx.RxViewGroup;
import io.nextop.view.ImageView;
import io.nextop.vm.ImageViewModel;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import javax.annotation.Nullable;

public class FlipFragment extends RxFragment {

    public static FlipFragment newInstance(Id flipId) {
        Bundle args = new Bundle();
        args.putString("flipId", flipId.toString());

        FlipFragment f = new FlipFragment();
        f.setArguments(args);
        return f;
    }


    Id flipId;
    Demo demo;
    FlipAdapter flipAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        flipId = Id.valueOf(getArguments().getString("flipId"));
        demo = (Demo) getActivity().getApplication();
        flipAdapter = new FlipAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_flip, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        View view = getView();

        final ListView listView = (ListView) view.findViewById(R.id.list);

        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        final View header = inflater.inflate(R.layout.view_flip_header, listView, false);
        final View footer = inflater.inflate(R.layout.view_flip_footer, listView, false);

        final ImageView largeView = (ImageView) view.findViewById(R.id.large);

        Button createButton = (Button) view.findViewById(R.id.create_button);


        listView.addHeaderView(header);
        listView.addFooterView(footer);
        listView.setAdapter(flipAdapter);

        bind(demo.getFlipVmm().get(flipId)).subscribe(flipAdapter);


        final PublishSubject<ImageViewModel> largeSource = PublishSubject.create();
        bind(largeSource.distinctUntilChanged().subscribe(new ImageView.Updater(largeView)));



        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FlipActivity) getActivity()).viewPager.setCurrentItem(1, true);
            }
        });



        // on layout change, set header and foot height to match list height
        listView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

                int height = bottom - top;
                if (height != header.getLayoutParams().height) {
                    header.getLayoutParams().height = height;
                    header.invalidate();
                }
                if (height != footer.getLayoutParams().height) {
                    footer.getLayoutParams().height = height;
                    footer.invalidate();
                }

            }
        });

        // FIXME animations here
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            int scrollState = SCROLL_STATE_IDLE;
            boolean visible = false;

            Subscription publishSubscription = null;
            int publishPosition = -1;



            void show() {
                if (!visible) {
                    visible = true;
                    // FIXME animate alpha in
                    largeView.setVisibility(View.VISIBLE);
                }
            }
            void hide() {
                if (visible) {
                    visible = false;
                    // FIXME animate alpha out
                    largeView.setVisibility(View.INVISIBLE);
                }
            }



            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

                this.scrollState = scrollState;
                switch (scrollState) {
                    case SCROLL_STATE_IDLE:
                    case SCROLL_STATE_TOUCH_SCROLL:
                        hide();
                        break;
                    default:
                        // ignore
                        break;
                }


            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // FIXME bind
                // FIXME fade bg of last element to black on scroll down

                switch (scrollState) {
                    case SCROLL_STATE_FLING:
                        if (publishLarge(firstVisibleItem, visibleItemCount)) {
                            show();
                        } else {
                            hide();
                        }
                        break;
                    default:
                        // ignore
                        break;
                }
            }

            boolean publishLarge(int firstVisibleItem, int visibleItemCount) {
                // publish large the frame closest to the center
                int centerPosition = firstVisibleItem + visibleItemCount / 2;
                if (publishLarge(centerPosition)) {
                    return true;
                }

                for (int i = 1; i <= visibleItemCount / 2; ++i) {
                    if (publishLarge(centerPosition + i) || publishLarge(centerPosition - i)) {
                        return true;
                    }
                }

                return false;
            }

            boolean publishLarge(int position) {
                ListAdapter adapter = listView.getAdapter();
                if (position < 0 || adapter.getCount() <= position) {
                    return false;
                }
                Object item = listView.getItemAtPosition(position);
                if (!(item instanceof Id)) {
                    return false;
                }
                if (publishPosition == position) {
                    return false;
                }
                Id frameId = (Id) item;

                publishPosition = position;


                if (null != publishSubscription) {
                    publishSubscription.unsubscribe();
                }
                publishSubscription = demo.getFrameVmm().get(frameId).map(new Func1<FrameViewModel, ImageViewModel>() {
                    @Override
                    public ImageViewModel call(FrameViewModel frameVm) {
                        return frameVm.imageVm;
                    }
                }).subscribe(largeSource);
                return true;
            }
        });

        // set up list adapter
        // set up scroll listener
        // set up logic to show fixed frame when scrolling fast

    }




    private class FlipAdapter extends BaseAdapter implements Observer<FlipViewModel> {
        @Nullable
        FlipViewModel flipVm = null;


        @Override
        public void onNext(FlipViewModel flipVm) {
            this.flipVm = flipVm;
            notifyDataSetChanged();
        }

        @Override
        public void onCompleted() {
            flipVm = null;
            notifyDataSetChanged();
        }

        @Override
        public void onError(Throwable e) {
            flipVm = null;
            notifyDataSetChanged();
        }



        @Override
        public int getCount() {
            return null != flipVm ? flipVm.size() : 0;
        }

        @Override
        public Id getItem(int position) {
            return flipVm.getFrameId(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).longHashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_flip_frame, parent, false);
            }

            Id frameId = getItem(position);

            final RxViewGroup rxVg = (RxViewGroup) convertView.findViewWithTag(RxViewGroup.TAG);
            if (null != rxVg) {
                rxVg.reset();
                updateIndefinitely(convertView, rxVg.bind(demo.getFrameVmm().get(frameId)));
            } else {
                // update immediate snapshot
                updateIndefinitely(convertView, demo.getFrameVmm().peek(frameId));
            }

            return convertView;
        }
        // assume the subscription will be managed via onComplete from a higher level
        // this code just assumes it runs until done
        private void updateIndefinitely(View view, Observable<FrameViewModel> frameVmSource) {
            Observable<ImageViewModel> imageVmSource = frameVmSource.map(new Func1<FrameViewModel, ImageViewModel>() {
                @Override
                public ImageViewModel call(FrameViewModel frameVm) {
                    return frameVm.imageVm;
                }
            }).distinctUntilChanged();

            ImageView imageView = (ImageView) view.findViewById(R.id.image);

            imageVmSource.subscribe(new ImageView.Updater(imageView));
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
