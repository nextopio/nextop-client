package io.nextop.rx;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.internal.util.SubscriptionList;

import java.util.Objects;

// connects to the view lifecycle (attached to window, detached from window)
public final class RxViewGroup extends FrameLayout implements RxLifecycleBinder {
    public static final Object TAG = new Object();


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

    }

    @Override
    public void bind(Subscription subscription) {

    }

    @Override
    public void bind(Func0<Subscription> source) {

    }

    @Override
    public void bind(Action1<SubscriptionList> source) {

    }

    @Override
    public <T> Observable<T> bind(Observable<T> source) {
        
        // FIXME
        return source;
    }
}
