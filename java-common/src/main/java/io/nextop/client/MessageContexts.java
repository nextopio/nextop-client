package io.nextop.client;

import io.nextop.util.NextopExecutors;
import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class MessageContexts {

    public static MessageContext executorContext(Executor executor) {
        return new SchedulerMessageContext(Schedulers.from(NextopExecutors.serialExecutor(executor)));
    }

    public static MessageContext executorContext() {
        return new SchedulerMessageContext(Schedulers.from(Executors.newSingleThreadExecutor()));
    }




    private static final class SchedulerMessageContext implements MessageContext {
        private final Scheduler scheduler;


        SchedulerMessageContext(Scheduler scheduler) {
            this.scheduler = scheduler;
        }


        @Override
        public void post(final Runnable r) {
            scheduler.createWorker().schedule(new Action0() {
                @Override
                public void call() {
                    r.run();
                }
            });
        }

        @Override
        public void postDelayed(final Runnable r, int delayMs) {
            scheduler.createWorker().schedule(new Action0() {
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
