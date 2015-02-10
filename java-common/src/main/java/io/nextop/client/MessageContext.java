package io.nextop.client;

public interface MessageContext {
    void post(Runnable r);
    void postDelayed(Runnable r, int delayMs);
}
