package io.nextop.client;

public interface ConnectionContext {
    Connection get(String host);
    void close();
    // TODO callbacks for system events
}
