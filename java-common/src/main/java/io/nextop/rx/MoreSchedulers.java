package io.nextop.rx;

import rx.Scheduler;

public class MoreSchedulers {


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
