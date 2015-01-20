package io.nextop.rx;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.internal.util.SubscriptionList;

public interface RxLifecycleBinder {

    void reset();


    void bind(Subscription subscription);
    void bind(Func0<Subscription> source);
    void bind(Action1<SubscriptionList> source);

    /** Wraps the given observable in an observable bound to the lifecyle of a container.
     * The wrapper allocates on subscribe and cleans up on unsubscribe.
     * Unsubscription of all can be forced with {@link #reset}.
     *
     * This ensures:
     * - the subscription is connected to the source when started
     * - the subscription is dropped from the source when stopped
     *   (onComplete is not called, because the subscription will resume when restarted) */
    <T> Observable<T> bind(Observable<T> source);


    /** Binds to an internal lifecycle onStart/onStop. */
    final class Lifted implements RxLifecycleBinder {



        public void onStart() {
            // FIXME
        }
        public void onStop() {
            // FIXME
        }




        public void reset() {

        }


        public void bind(Subscription subscription) {

        }
        public void bind(Func0<Subscription> source) {

        }
        public void bind(Action1<SubscriptionList> source) {

        }
        public <T> Observable<T> bind(Observable<T> source) {
            // FIXME
            return source;
        }




    }
}
