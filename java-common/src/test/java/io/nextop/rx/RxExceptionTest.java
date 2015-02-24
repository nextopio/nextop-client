package io.nextop.rx;

import junit.framework.TestCase;
import rx.Notification;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.exceptions.OnErrorFailedException;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** testing exception handling in RxJava */
public class RxExceptionTest extends TestCase {

    public void testExceptionSubjectPost() {
        // test the behavior of surfacing exceptions from a subject
        // onNext after subscriber

        BehaviorSubject<Integer> subject = BehaviorSubject.create();

        final List<Notification<Integer>> notifications = new ArrayList<Notification<Integer>>(4);

        Subscription s = subject.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer t) {
                notifications.add(Notification.createOnNext(t));
                throw new RuntimeException("onNext " + t);
            }

            @Override
            public void onCompleted() {
                notifications.add(Notification.<Integer>createOnCompleted());
            }

            @Override
            public void onError(Throwable e) {
                notifications.add(Notification.<Integer>createOnError(e));
            }
        });

        subject.onNext(0);

        assertEquals(2, notifications.size());
        Notification n0 = notifications.get(0);
        assertEquals(Notification.Kind.OnNext, n0.getKind());
        assertEquals(0, n0.getValue());
        Notification n1 = notifications.get(1);
        assertEquals(Notification.Kind.OnError, n1.getKind());
        assertTrue(n1.getThrowable() instanceof RuntimeException);
        assertEquals("onNext 0", n1.getThrowable().getMessage());
    }

    public void testExceptionSubjectPre() {
        // test the behavior of surfacing exceptions from a subject
        // onNext before subscriber

        BehaviorSubject<Integer> subject = BehaviorSubject.create();
        subject.onNext(0);

        final List<Notification<Integer>> notifications = new ArrayList<Notification<Integer>>(4);

        Subscription s = subject.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer t) {
                notifications.add(Notification.createOnNext(t));
                throw new RuntimeException("onNext " + t);
            }

            @Override
            public void onCompleted() {
                notifications.add(Notification.<Integer>createOnCompleted());
            }

            @Override
            public void onError(Throwable e) {
                notifications.add(Notification.<Integer>createOnError(e));
            }
        });


        assertEquals(2, notifications.size());
        Notification n0 = notifications.get(0);
        assertEquals(Notification.Kind.OnNext, n0.getKind());
        assertEquals(0, n0.getValue());
        Notification n1 = notifications.get(1);
        assertEquals(Notification.Kind.OnError, n1.getKind());
        assertTrue(n1.getThrowable() instanceof RuntimeException);
        assertEquals("onNext 0", n1.getThrowable().getMessage());
    }


    public void testExceptionSubjectAction() {
        // test the behavior of surfacing exceptions from a subject


        BehaviorSubject<Integer> subject = BehaviorSubject.create();

        final List<Notification<Integer>> notifications = new ArrayList<Notification<Integer>>(4);

        Subscription s = subject.subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                notifications.add(Notification.createOnNext(t));
                throw new RuntimeException("call " + t);
            }
        });

        try {
            subject.onNext(0);
            // (unreachable) expect an exception to be thrown
            fail();
        } catch (RuntimeException e) {
            assertEquals("call 0", e.getMessage());
        }
    }



    public void testExceptionSubjectObserverCustomThrow() {
        // setup:
        // subject -> observer
        // shows that an exception in observer#onNext will call observer#onError,
        // and that an unahndled exception in observer#onError will come back to the caller


        BehaviorSubject<Integer> subject = BehaviorSubject.create();

        final List<Notification<Integer>> notifications = new ArrayList<Notification<Integer>>(4);

        Subscription s = subject.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer t) {
                notifications.add(Notification.createOnNext(t));
                throw new RuntimeException("onNext " + t);
            }

            @Override
            public void onCompleted() {
                notifications.add(Notification.<Integer>createOnCompleted());
            }

            @Override
            public void onError(Throwable e) {
                notifications.add(Notification.<Integer>createOnError(e));
                if (e instanceof RuntimeException) {
                    // TODO assume an error path from onNext
                    throw (RuntimeException) e;
                } else if (e instanceof Error) {
                    // TODO assume an error path from onNext
                    throw (Error) e;
                } else {
                    // TODO
                    // a normal error path?
                }
            }
        });

        try {
            subject.onNext(0);
            // (unreachable) expect an exception to be thrown
            fail();
        } catch (RuntimeException e) {
            // OnErrorFailedException
            //   CompositeException
            //     CompositeException$CompositeExceptionCausalChain
            //       <call 0>

            assertTrue(e instanceof OnErrorFailedException);
            assertTrue(e.getCause() instanceof CompositeException);
            // TODO CompositeException$CompositeExceptionCausalChain
            assertEquals("onNext 0", e.getCause().getCause().getCause().getMessage());
        }
    }



    public void testExceptionSubjectIndirect() {
        // setup:
        // subject -> subject -> observer
        // shows that an exception in observer#onNext will call observer#onError,
        // and that an unhandled exception in observer#onError will *not* come back to the caller
        // (is this to prevent an infinite loop   obs#onNext -> obs#onError -> subjA/B#onError -> obs#onError ... ?)

        PublishSubject<Integer> entry = PublishSubject.create();

        BehaviorSubject<Integer> subject = BehaviorSubject.create();

        entry.map(new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer t) {
                return t + 1;
            }
        }).subscribe(subject);


        final List<Notification<Integer>> notifications = new ArrayList<Notification<Integer>>(4);

        Subscription s = subject.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer t) {
                notifications.add(Notification.createOnNext(t));
                throw new RuntimeException("onNext " + t);
            }

            @Override
            public void onCompleted() {
                notifications.add(Notification.<Integer>createOnCompleted());
            }

            @Override
            public void onError(Throwable e) {
                notifications.add(Notification.<Integer>createOnError(e));
                if (e instanceof RuntimeException) {
                    // TODO assume an error path from onNext
                    throw (RuntimeException) e;
                } else if (e instanceof Error) {
                    // TODO assume an error path from onNext
                    throw (Error) e;
                } else {
                    // TODO
                    // a normal error path?
                }
            }
        });

        entry.onNext(0);
        // nothing thrown ...

        assertEquals(2, notifications.size());
        Notification n0 = notifications.get(0);
        assertEquals(Notification.Kind.OnNext, n0.getKind());
        assertEquals(/* 0 + 1 */ 1, n0.getValue());
        Notification n1 = notifications.get(1);
        assertEquals(Notification.Kind.OnError, n1.getKind());
    }



    public void testExceptionSubjectIndirectScheduler() throws Exception {
        // setup:
        // subject -> subject -> (observe on scheduler) -> observer
        // shows that an exception in observer#onNext will call observer#onError,
        // and that an unhandled exception in observer#onError will post to the uncaught exception handler

        PublishSubject<Integer> entry = PublishSubject.create();

        BehaviorSubject<Integer> subject = BehaviorSubject.create();

        Scheduler scheduler = Schedulers.io();


        entry.map(new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer t) {
                return t + 1;
            }
        }).subscribe(subject);


        final List<Notification<Integer>> notifications = Collections.synchronizedList(new ArrayList<Notification<Integer>>(4));
        final List<Throwable> uncaughtException = Collections.synchronizedList(new ArrayList<Throwable>(4));


        Subscription s = subject.observeOn(scheduler).subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer t) {
                notifications.add(Notification.createOnNext(t));
                throw new RuntimeException("onNext " + t);
            }

            @Override
            public void onCompleted() {
                notifications.add(Notification.<Integer>createOnCompleted());
            }

            @Override
            public void onError(Throwable e) {
                notifications.add(Notification.<Integer>createOnError(e));
                if (e instanceof RuntimeException) {
                    // TODO assume an error path from onNext
                    throw (RuntimeException) e;
                } else if (e instanceof Error) {
                    // TODO assume an error path from onNext
                    throw (Error) e;
                } else {
                    // TODO
                    // a normal error path?
                }
            }
        });

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                uncaughtException.add(e);
            }
        });


        entry.onNext(0);
        // expect nothing to be thrown because async

        // wait a bit for scheduler to publish notifications
        Thread.sleep(2000);

        // expect the exception to come in through the uncaught exception handler for the thread
        assertEquals(1, uncaughtException.size());

        assertEquals(2, notifications.size());
        Notification n0 = notifications.get(0);
        assertEquals(Notification.Kind.OnNext, n0.getKind());
        assertEquals(/* 0 + 1 */ 1, n0.getValue());
        Notification n1 = notifications.get(1);
        assertEquals(Notification.Kind.OnError, n1.getKind());
    }



}
