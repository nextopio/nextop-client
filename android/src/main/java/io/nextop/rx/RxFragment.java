package io.nextop.rx;

import android.app.Fragment;
import rx.Observable;
import rx.Subscription;

// connects to the fragment lifecycle (on resume, on pause)
public class RxFragment extends Fragment implements RxLifecycleBinder {


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
