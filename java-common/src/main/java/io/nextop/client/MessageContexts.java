package io.nextop.client;

import io.nextop.rx.MoreSchedulers;
import io.nextop.util.MoreExecutors;
import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class MessageContexts {

    public static MessageContext create(Executor executor) {
        return SchedulerMessageContext.create(MoreSchedulers.serial(executor));
    }

    public static MessageContext create(Scheduler scheduler) {
        return SchedulerMessageContext.create(scheduler);
    }

    public static MessageContext create() {
        return SchedulerMessageContext.create(Schedulers.from(Executors.newSingleThreadExecutor()));
    }




    private static final class SchedulerMessageContext implements MessageContext {
        static SchedulerMessageContext create(Scheduler scheduler) {
            return new SchedulerMessageContext(scheduler.createWorker());
        }


        private final Scheduler.Worker worker;
        private final Scheduler scheduler;


        SchedulerMessageContext(Scheduler.Worker worker) {
            this.worker = worker;
            scheduler = MoreSchedulers.from(worker);
        }


        @Override
        public void post(final Runnable r) {
            worker.schedule(new Action0() {
                @Override
                public void call() {
                    r.run();
                }
            });
        }

        @Override
        public void postDelayed(final Runnable r, int delayMs) {
            worker.schedule(new Action0() {
                @Override
                public void call() {
                    r.run();
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        }

        @Override
        public Scheduler getScheduler() {
            return scheduler;

        }
    }


}
