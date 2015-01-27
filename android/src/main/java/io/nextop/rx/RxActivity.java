package io.nextop.rx;

import android.app.Activity;
import rx.Observable;
import rx.Subscription;

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

    @Override
    public void onResume() {
        super.onResume();
        liftedRxLifecycleBinder.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        liftedRxLifecycleBinder.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        liftedRxLifecycleBinder.close();
    }

}
