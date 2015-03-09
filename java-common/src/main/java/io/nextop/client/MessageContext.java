package io.nextop.client;

import rx.Scheduler;

public interface MessageContext {
    void post(Runnable r);
    void postDelayed(Runnable r, int delayMs);
    Scheduler getScheduler();
}
