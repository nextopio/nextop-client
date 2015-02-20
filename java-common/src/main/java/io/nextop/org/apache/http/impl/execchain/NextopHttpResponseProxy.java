package io.nextop.org.apache.http.impl.execchain;

import io.nextop.org.apache.http.HttpResponse;

/** package hack to expand the visibility of the base class */
public class NextopHttpResponseProxy extends HttpResponseProxy {
    public NextopHttpResponseProxy(final HttpResponse original, final ConnectionHolder connHolder) {
        super(original, connHolder);
    }
}
