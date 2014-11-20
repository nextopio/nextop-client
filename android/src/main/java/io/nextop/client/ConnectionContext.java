package io.nextop.client;

public interface ConnectionContext {
    Connection get(String host);

    void onCreate();
    /** not guaranteed to be called */
    void onTerminate();
}
