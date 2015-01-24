package io.nextop.demo.flip;

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
import rx.functions.Action1;
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
    Flip flip;
    FlipAdapter flipAdapter;
    ListView listView;

    boolean scrollToEnd = false;

    int targetHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        flipId = Id.valueOf(getArguments().getString("flipId"));
        flip = (Flip) getActivity().getApplication();
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

        listView = (ListView) view.findViewById(R.id.list);

        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        final View header = inflater.inflate(R.layout.view_flip_header, listView, false);
        final View footer = inflater.inflate(R.layout.view_flip_footer, listView, false);

        final ImageView largeView = (ImageView) view.findViewById(R.id.large);

        ImageButton createButton = (ImageButton) view.findViewById(R.id.create_button);


        listView.addHeaderView(header);
        listView.addFooterView(footer);
        listView.setAdapter(flipAdapter);

        bind(flip.getFlipVmm().get(flipId)).subscribe(flipAdapter);


        final PublishSubject<ImageViewModel> largeSource = PublishSubject.create();
        bind(largeSource).distinctUntilChanged().subscribe(new ImageView.Updater(largeView));



        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FlipActivity) getActivity()).startRecording();
            }
        });



        // on layout change, set header and foot height to match list height
        listView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

                targetHeight = bottom - top;
                if (targetHeight != header.getLayoutParams().height) {
                    header.getLayoutParams().height = targetHeight;
                    header.invalidate();
                }
                if (targetHeight != footer.getLayoutParams().height) {
                    footer.getLayoutParams().height = targetHeight;
                    footer.invalidate();
                }

            }
        });

        // FIXME animations here
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            int scrollState = SCROLL_STATE_IDLE;
            boolean visible = false;

            int publishIndex = -1;



            void show() {
//                if (!visible) {
//                    visible = true;
//
//                    if (View.VISIBLE != largeView.getVisibility()) {
////                        largeView.setAlpha(0.f);
//                        largeView.setVisibility(View.VISIBLE);
//                    }
//
//                    float a = largeView.getAlpha();
//
//                    ObjectAnimator animator = ObjectAnimator.ofFloat(largeView, "alpha", a, 1.f);
//                    animator.setAutoCancel(true);
//                    animator.setDuration(Math.round((1.f - a) * 200));
//                    animator.setInterpolator(new DecelerateInterpolator());
//                    animator.start();
//
//                }
                if (!visible) {
                    visible = true;
                    largeView.setVisibility(View.VISIBLE);
                }
            }
            void hide() {
//                if (visible) {
//                    visible = false;
//
//                    if (View.VISIBLE == largeView.getVisibility()) {
//
//                        float a = largeView.getAlpha();
//
//                        ObjectAnimator animator = ObjectAnimator.ofFloat(largeView, "alpha", a, 0.f);
//                        animator.setAutoCancel(true);
//                        animator.setDuration(Math.round(a * 200));
//                        animator.setInterpolator(new AccelerateInterpolator());
//
//                        animator.addListener(new Animator.AnimatorListener() {
//                            @Override
//                            public void onAnimationEnd(Animator animation) {
//                                largeView.setVisibility(View.INVISIBLE);
//                            }
//
//                            @Override
//                            public void onAnimationStart(Animator animation) {
//
//                            }
//
//                            @Override
//                            public void onAnimationCancel(Animator animation) {
//
//                            }
//
//                            @Override
//                            public void onAnimationRepeat(Animator animation) {
//
//                            }
//                        });
//                        animator.start();
//
//                    }
//
//                }
                if (visible) {
                    visible = false;
                    largeView.setVisibility(View.INVISIBLE);
                }
            }



            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

                this.scrollState = scrollState;
//                switch (scrollState) {
//                    case SCROLL_STATE_IDLE:
//                    case SCROLL_STATE_TOUCH_SCROLL:
//                        hide();
//                        break;
//                    default:
//                        // ignore
//                        break;
//                }


            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // FIXME bind
                // FIXME fade bg of last element to black on scroll down

                if (publishLarge(firstVisibleItem, visibleItemCount)) {
                    show();
                } else {
                    hide();
                }

//                switch (scrollState) {
//                    case SCROLL_STATE_FLING:
//                        if (publishLarge(firstVisibleItem, visibleItemCount)) {
//                            show();
//                        } else {
//                            hide();
//                        }
//                        break;
//                    default:
//                        // ignore
//                        break;
//                }
            }

            boolean publishLarge(int firstVisibleItem, int visibleItemCount) {
                // publish large the frame closest to the center
//                int centerPosition = firstVisibleItem + visibleItemCount / 2;
//                if (publishLarge(centerPosition)) {
//                    return true;
//                }
//
//                for (int i = 1; i <= visibleItemCount / 2; ++i) {
//                    if (publishLarge(centerPosition + i) || publishLarge(centerPosition - i)) {
//                        return true;
//                    }
//                }
//
//                return false;

                int index = indexOf(firstVisibleItem);
                if (0 <= index && 0 <= indexOf(firstVisibleItem + visibleItemCount - 1)) {
                    publishLarge(index);
                    return true;
                } else {
                    return false;
                }
            }

            int indexOf(int position) {
                int index = position - listView.getHeaderViewsCount();
                if (index < 0 || flipAdapter.getCount() <= index) {
                    return -1;
                }
                return index;
            }

            void publishLarge(int index) {
                if (publishIndex != index) {
                    publishIndex = index;

                    Id frameId = flipAdapter.getItem(index);

                    largeSource.onNext(flipAdapter.flipVm.getFrameVm(frameId).imageVm);
                }
            }
        });


        updateHeaderIndefinitely(header, bind(flip.getFlipInfoVmm().get(flipId)));


        // set up list adapter
        // set up scroll listener
        // set up logic to show fixed frame when scrolling fast

        scrollToEnd();
    }


    private void updateHeaderIndefinitely(View view, Observable<FlipInfoViewModel> flipInfoVmSource) {
        final TextView intro = (TextView) view.findViewById(R.id.intro);
        Observable<String> introSource = flipInfoVmSource.map(new Func1<FlipInfoViewModel, String>() {
            @Override
            public String call(FlipInfoViewModel flipInfoVm) {
                return flipInfoVm.intro;
            }
        });
        introSource.subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                intro.setText(s);
            }
        });
    }



    void onStartRecording() {
        // Do nothing
    }
    void onStopRecording() {
        // Do nothing

        scrollToEnd = true;
        scrollToEnd();
    }

    void scrollToEnd() {
        if (scrollToEnd && null != listView) {
            scrollToEnd = false;

            int n = flipAdapter.getCount();
            int index = n - 1;
            for (; 0 <= index; --index) {
                @Nullable FrameViewModel frameVm = flipAdapter.flipVm.getFrameVm(flipAdapter.getItem(index));
                if (null != frameVm && null != frameVm.imageVm.uri) {
                    break;
                }
            }
            if (index + 1 < n) {
                listView.setSelection(listView.getHeaderViewsCount() + index + 1);
            }
        }
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
                @Nullable RxViewGroup rxVg = (RxViewGroup) convertView.findViewWithTag(RxViewGroup.TAG);
                if (null != rxVg) {
                    rxVg.cascadeDispose(FlipFragment.this);
                }
            }

            convertView.getLayoutParams().height = targetHeight;


            final Id frameId = getItem(position);

            @Nullable RxViewGroup rxVg = (RxViewGroup) convertView.findViewWithTag(RxViewGroup.TAG);
            if (null != rxVg) {
                rxVg.reset();
                updateIndefinitely(convertView, rxVg.bind(flip.getFlipVmm().get(flipId).map(new Func1<FlipViewModel, FrameViewModel>() {
                    @Override
                    public FrameViewModel call(FlipViewModel flipVm) {
                        return flipVm.frameVms.get(frameId);
                    }
                })));
            } else {
                // update immediate snapshot
                updateIndefinitely(convertView, flip.getFlipVmm().peek(flipId).map(new Func1<FlipViewModel, FrameViewModel>() {
                    @Override
                    public FrameViewModel call(FlipViewModel flipVm) {
                        return flipVm.frameVms.get(frameId);
                    }
                }));
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
