package io.nextop.rx;

import io.nextop.util.MoreExecutors;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MoreSchedulers {
    public static Scheduler serial() {
        return Schedulers.from(Executors.newSingleThreadExecutor());
    }

    public static Scheduler serial(Executor executor) {
        return Schedulers.from(MoreExecutors.serial(executor));
    }

    public static Scheduler serial(Scheduler scheduler) {
        return from(scheduler.createWorker());
    }

    public static Scheduler from(final Scheduler.Worker worker) {
        return new Scheduler() {
            @Override
            public Worker createWorker() {
                return worker;
            }

            @Override
            public long now() {
                return worker.now();
            }
        };
    }

}
