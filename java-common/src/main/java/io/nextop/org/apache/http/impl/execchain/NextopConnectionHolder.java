package io.nextop.org.apache.http.impl.execchain;

import io.nextop.org.apache.http.HttpClientConnection;
import io.nextop.org.apache.http.conn.HttpClientConnectionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** package hack to expand the visibility of the base class */
public final class NextopConnectionHolder extends ConnectionHolder {
    static final Log log = LogFactory.getLog(NextopConnectionHolder.class);

    public NextopConnectionHolder(
            final HttpClientConnectionManager manager,
            final HttpClientConnection managedConn) {
        super(log, manager, managedConn);
    }
}
