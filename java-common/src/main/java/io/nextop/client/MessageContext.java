package io.nextop.client;

import rx.Scheduler;

/** thread-safe */
public interface MessageContext {

    void post(Runnable r);

    void postDelayed(Runnable r, int delayMs);

    /** this context should be correctly serialized,
     * so multiple #createWorker should execute jobs in order of input across all */
    Scheduler getScheduler();
}
