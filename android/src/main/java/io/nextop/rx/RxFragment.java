package io.nextop.rx;

import android.app.Fragment;
import rx.Observable;

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
