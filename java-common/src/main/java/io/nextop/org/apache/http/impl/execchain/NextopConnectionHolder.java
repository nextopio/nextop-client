package io.nextop.org.apache.http.impl.execchain;

import io.nextop.org.apache.http.HttpClientConnection;
import io.nextop.org.apache.http.conn.HttpClientConnectionManager;

/** package hack to expand the visibility of the base class */
public final class NextopConnectionHolder extends ConnectionHolder {
    public NextopConnectionHolder(
            final HttpClientConnectionManager manager,
            final HttpClientConnection managedConn) {
        super(NextopLogger.EMPTY, manager, managedConn);
    }
}
