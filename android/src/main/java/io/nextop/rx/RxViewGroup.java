package io.nextop.rx;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import rx.Observable;

import javax.annotation.Nullable;

// connects to the view lifecycle (attached to window, detached from window)
public final class RxViewGroup extends FrameLayout implements RxLifecycleBinder {
    public static final Object TAG = new Object();


    private final RxLifecycleBinder.Lifted liftedRxLifecycleBinder = new RxLifecycleBinder.Lifted();


    public RxViewGroup(Context context) {
        super(context);
        init();
    }
    public RxViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public RxViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public RxViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
    private void init() {
        setTag(TAG);
    }




    @Override
    public void reset() {
        liftedRxLifecycleBinder.reset();
    }

    @Override
    public <T> Observable<T> bind(Observable<T> source) {
        return liftedRxLifecycleBinder.bind(source);
    }


    public void onDispose() {
        liftedRxLifecycleBinder.onDestroy();
    }

    public void cascadeDispose(@Nullable RxLifecycleBinder parent) {
        liftedRxLifecycleBinder.cascadeDestroy(parent);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        liftedRxLifecycleBinder.onStart();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        liftedRxLifecycleBinder.onStop();
    }





}
