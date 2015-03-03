package io.nextop.client.test;

// base for most node tests,
// that puts node creation and sending on the correct scheduler,
// and provides a way to await termination and do a final verification

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subjects.PublishSubject;

import javax.annotation.Nullable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public abstract class WorkloadRunner implements Action0 {
    protected final Scheduler scheduler;

    @Nullable
    volatile Exception e = null;
    final Semaphore end = new Semaphore(0);

    int n = 200;

    protected int timeoutMs = 10000;


    public WorkloadRunner(Scheduler scheduler) {
        this.scheduler = scheduler;
    }


    public void start() {
        scheduler.createWorker().schedule(this);
    }


    public void join() throws Exception {
        end.acquire();
        if (null != e) {
            throw e;
        }
    }



    void end(@Nullable Exception e) {
        this.e = e;
        end.release();
    }


    @Override
    public void call() {
        try {
            run();
            Subscription s = observeEnd().doOnCompleted(new Action0() {
                @Override
                public void call() {
                    try {
                        try {
                            check();
                        } finally {
                            cleanup();
                        }
                        end(null);
                    } catch (Exception e) {
                        end(e);
                    }
                }
            }).observeOn(scheduler).subscribe();
        } catch (Exception e) {
            end(e);
        }
    }


    // provides an observable that, when completes, the test should complete
    protected Observable<Void> observeEnd() {
        final PublishSubject<Void> subject = PublishSubject.create();
        return subject.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                scheduler.createWorker().schedule(new Action0() {
                    @Override
                    public void call() {
                        subject.onCompleted();
                    }
                },
                        timeoutMs, TimeUnit.MILLISECONDS);
            }
        }).share();
    }


    // run on scheduler
    public abstract void run() throws Exception;
    // run on scheduler
    public abstract void check() throws Exception;

    public void cleanup() throws Exception {
    }
}
