package io.nextop.test.rx;

import io.nextop.rx.RxLifecycleBinder;
import junit.framework.TestCase;
import rx.Observer;
import rx.Subscription;
import rx.subjects.BehaviorSubject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

public class RxLifecycleBinderTest extends TestCase {



    public void testBindAdapter() {
        final int[] nextCount = {0};
        final int[] completedCount = {0};
        final int[] errorCount = {0};

        final BehaviorSubject<Integer> subject = BehaviorSubject.create();
        subject.onNext(0);

        RxLifecycleBinder.Lifted binder = new RxLifecycleBinder.Lifted();

        int d = 4;
        for (int i = 0; i < d; ++i) {
            binder.bind(subject).subscribe(new Observer<Integer>() {
                @Override
                public void onNext(Integer integer) {
                    ++nextCount[0];
                }

                @Override
                public void onCompleted() {
                    ++completedCount[0];
                }

                @Override
                public void onError(Throwable e) {
                    ++errorCount[0];
                }
            });
        }

        assertEquals(0, nextCount[0]);
        assertEquals(0, completedCount[0]);
        assertEquals(0, errorCount[0]);

        // resume it
        binder.connect();

        assertEquals(d, nextCount[0]);
        assertEquals(0, completedCount[0]);
        assertEquals(0, errorCount[0]);

        // pause it, publish some updates
        binder.disconnect();
        subject.onNext(1);
        subject.onNext(2);

        assertEquals(d, nextCount[0]);
        assertEquals(0, completedCount[0]);
        assertEquals(0, errorCount[0]);

        // resume it
        binder.connect();

        assertEquals(2 * d, nextCount[0]);
        assertEquals(0, completedCount[0]);
        assertEquals(0, errorCount[0]);

        // pause it, close the source
        binder.disconnect();
        subject.onCompleted();

        assertEquals(2 * d, nextCount[0]);
        assertEquals(0, completedCount[0]);
        assertEquals(0, errorCount[0]);

        binder.connect();

        assertEquals(2 * d, nextCount[0]);
        assertEquals(d, completedCount[0]);
        assertEquals(0, errorCount[0]);

    }

    public void testBindSubscription() {
        final int[] nextCount = {0};
        final int[] completedCount = {0};
        final int[] errorCount = {0};

        final BehaviorSubject<Integer> subject = BehaviorSubject.create();
        subject.onNext(0);

        RxLifecycleBinder.Lifted binder = new RxLifecycleBinder.Lifted();

        Collection<Subscription> subscriptions = new LinkedList<Subscription>();

        int d = 4;
        for (int i = 0; i < d; ++i) {
            // this binds a subscription in flight
            // versus wrapping the observable
            Subscription subscription = subject.subscribe(new Observer<Integer>() {
                @Override
                public void onNext(Integer integer) {
                    ++nextCount[0];
                }

                @Override
                public void onCompleted() {
                    ++completedCount[0];
                }

                @Override
                public void onError(Throwable e) {
                    ++errorCount[0];
                }
            });
            subscriptions.add(subscription);
            binder.bind(subscription);
        }

        assertEquals(d, nextCount[0]);
        assertEquals(0, completedCount[0]);
        assertEquals(0, errorCount[0]);

        // resume it
        binder.connect();

        assertEquals(d, nextCount[0]);
        assertEquals(0, completedCount[0]);
        assertEquals(0, errorCount[0]);

        // pause it, publish some updates
        binder.disconnect();
        subject.onNext(1);
        subject.onNext(2);

        assertEquals(3 * d, nextCount[0]);
        assertEquals(0, completedCount[0]);
        assertEquals(0, errorCount[0]);

        binder.reset();

        for (Subscription subscription : subscriptions) {
            assertTrue(subscription.isUnsubscribed());
        }

        // unsubscribing does not trigger an onComplete
        assertEquals(3 * d, nextCount[0]);
        assertEquals(0, completedCount[0]);
        assertEquals(0, errorCount[0]);
    }

    public void testReset() {
        final int[] nextCount = {0};
        final int[] completedCount = {0};
        final int[] errorCount = {0};

        final BehaviorSubject<Integer> subject = BehaviorSubject.create();
        subject.onNext(0);

        RxLifecycleBinder.Lifted binder = new RxLifecycleBinder.Lifted();

        int d = 4;
        for (int i = 0; i < d; ++i) {
            binder.bind(subject).subscribe(new Observer<Integer>() {
                @Override
                public void onNext(Integer integer) {
                    ++nextCount[0];
                }

                @Override
                public void onCompleted() {
                    ++completedCount[0];
                }

                @Override
                public void onError(Throwable e) {
                    ++errorCount[0];
                }
            });
        }

        assertEquals(0, nextCount[0]);
        assertEquals(0, completedCount[0]);
        assertEquals(0, errorCount[0]);

        // resume it
        binder.connect();

        assertEquals(d, nextCount[0]);
        assertEquals(0, completedCount[0]);
        assertEquals(0, errorCount[0]);

        binder.reset();

        assertEquals(d, nextCount[0]);
        assertEquals(d, completedCount[0]);
        assertEquals(0, errorCount[0]);
    }

}
