package io.nextop.rx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import rx.Observable;
import rx.Subscription;

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

    @SuppressLint("NewApi")
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
    public boolean reset(Object id) {
        return liftedRxLifecycleBinder.reset(id);
    }

    @Override
    public <T> Observable<T> bind(Observable<T> source) {
        return liftedRxLifecycleBinder.bind(source);
    }

    @Override
    public void bind(Subscription sub) {
        liftedRxLifecycleBinder.bind(sub);
    }

    @Override
    public void unsubscribe() {
        liftedRxLifecycleBinder.unsubscribe();
    }

    @Override
    public boolean isUnsubscribed() {
        return liftedRxLifecycleBinder.isUnsubscribed();
    }


    public void onDispose() {
        liftedRxLifecycleBinder.close();
    }

    public void cascadeDispose(@Nullable RxLifecycleBinder parent) {
        liftedRxLifecycleBinder.cascadeUnsubscribe(parent);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        evalBinder();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        evalBinder();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        evalBinder();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        evalBinder();
    }


    private void evalBinder() {
        boolean connect;
        if (View.VISIBLE == getWindowVisibility()) {
            if (View.VISIBLE == getVisibility()) {
                boolean visible = true;
                for (ViewParent p = getParent(); visible && p instanceof View; p = p.getParent()) {
                    visible &= View.VISIBLE == ((View) p).getVisibility();
                }
                connect = visible;
            } else {
                connect = false;
            }
        } else {
            connect = false;
        }
        if (connect) {
            liftedRxLifecycleBinder.connect(this);
        } else {
            liftedRxLifecycleBinder.disconnect();
        }
    }
}
