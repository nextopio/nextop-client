package io.nextop.client.http;

import io.nextop.org.apache.http.HttpException;
import io.nextop.org.apache.http.HttpHost;
import io.nextop.org.apache.http.HttpRequest;
import io.nextop.org.apache.http.client.ClientProtocolException;
import io.nextop.org.apache.http.client.config.RequestConfig;
import io.nextop.org.apache.http.client.methods.CloseableHttpResponse;
import io.nextop.org.apache.http.client.methods.Configurable;
import io.nextop.org.apache.http.client.methods.HttpExecutionAware;
import io.nextop.org.apache.http.client.methods.HttpRequestWrapper;
import io.nextop.org.apache.http.client.protocol.HttpClientContext;
import io.nextop.org.apache.http.conn.ClientConnectionManager;
import io.nextop.org.apache.http.conn.ClientConnectionRequest;
import io.nextop.org.apache.http.conn.HttpClientConnectionManager;
import io.nextop.org.apache.http.conn.ManagedClientConnection;
import io.nextop.org.apache.http.conn.routing.HttpRoute;
import io.nextop.org.apache.http.conn.scheme.SchemeRegistry;
import io.nextop.org.apache.http.impl.DefaultConnectionReuseStrategy;
import io.nextop.org.apache.http.impl.client.CloseableHttpClient;
import io.nextop.org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import io.nextop.org.apache.http.params.BasicHttpParams;
import io.nextop.org.apache.http.params.HttpParams;
import io.nextop.org.apache.http.protocol.BasicHttpContext;
import io.nextop.org.apache.http.protocol.HttpContext;
import io.nextop.org.apache.http.protocol.HttpRequestExecutor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

// TODO remove
// TODO PoolingHttpClientConnectionManager
// see MinimalHttpClient
class NextopHttpClient extends CloseableHttpClient {
    private final HttpClientConnectionManager connManager;
    private final NextopClientExec requestExecutor;
    private final HttpParams params;

    public NextopHttpClient(
            final HttpClientConnectionManager connManager) {
        this.connManager = connManager;
        this.requestExecutor = new NextopClientExec(new HttpRequestExecutor(),
                connManager,
                DefaultConnectionReuseStrategy.INSTANCE,
                DefaultConnectionKeepAliveStrategy.INSTANCE);
        this.params = new BasicHttpParams();
    }

    @Override
    protected CloseableHttpResponse doExecute(
            final HttpHost target,
            final HttpRequest request,
            final HttpContext context) throws IOException, ClientProtocolException {
        HttpExecutionAware execAware = null;
        if (request instanceof HttpExecutionAware) {
            execAware = (HttpExecutionAware) request;
        }
        try {
            final HttpRequestWrapper wrapper = HttpRequestWrapper.wrap(request);
            final HttpClientContext clientContext = HttpClientContext.adapt(
                    null != context ? context : new BasicHttpContext());
            final HttpRoute route = new HttpRoute(target);
            RequestConfig config = null;
            if (request instanceof Configurable) {
                config = ((Configurable) request).getConfig();
            }
            if (config != null) {
                clientContext.setRequestConfig(config);
            }
            return requestExecutor.execute(route, wrapper, clientContext, execAware);
        } catch (final HttpException httpException) {
            throw new ClientProtocolException(httpException);
        }
    }

    @Override
    public HttpParams getParams() {
        return this.params;
    }

    @Override
    public void close() {
        this.connManager.shutdown();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return new ClientConnectionManager() {
            @Override
            public void shutdown() {
                connManager.shutdown();
            }

            @Override
            public ClientConnectionRequest requestConnection(
                    final HttpRoute route, final Object state) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void releaseConnection(
                    final ManagedClientConnection conn,
                    final long validDuration, final TimeUnit timeUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SchemeRegistry getSchemeRegistry() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void closeIdleConnections(final long idletime, final TimeUnit tunit) {
                connManager.closeIdleConnections(idletime, tunit);
            }

            @Override
            public void closeExpiredConnections() {
                connManager.closeExpiredConnections();
            }
        };
    }
}
