package io.nextop.rx;

import android.app.Activity;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.internal.util.SubscriptionList;

public class RxActivity extends Activity implements RxLifecycleBinder {





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
