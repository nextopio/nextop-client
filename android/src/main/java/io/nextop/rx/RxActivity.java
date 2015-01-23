package io.nextop.rx;

import android.app.Activity;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.internal.util.SubscriptionList;

public class RxActivity extends Activity implements RxLifecycleBinder {


    private final RxLifecycleBinder.Lifted liftedRxLifecycleBinder = new RxLifecycleBinder.Lifted();






    @Override
    public void reset() {
        liftedRxLifecycleBinder.reset();
    }

    @Override
    public <T> Observable<T> bind(Observable<T> source) {
        return liftedRxLifecycleBinder.bind(source);
    }


    @Override
    public void onResume() {
        super.onResume();
        liftedRxLifecycleBinder.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        liftedRxLifecycleBinder.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        liftedRxLifecycleBinder.onDestroy();
    }

}
