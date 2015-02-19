package io.nextop.client.http;

import io.nextop.Message;
import io.nextop.client.AbstractMessageControlNode;
import io.nextop.client.MessageControl;
import io.nextop.client.MessageControlMetrics;
import io.nextop.client.MessageControlState;
import io.nextop.client.retry.SendStrategy;
import io.nextop.org.apache.http.*;
import io.nextop.org.apache.http.client.HttpClient;
import io.nextop.org.apache.http.client.methods.HttpUriRequest;
import io.nextop.org.apache.http.impl.client.DefaultHttpClient;
import io.nextop.org.apache.http.impl.conn.DefaultManagedHttpClientConnection;
import io.nextop.org.apache.http.protocol.HttpContext;
import io.nextop.org.apache.http.protocol.HttpCoreContext;
import io.nextop.org.apache.http.protocol.HttpRequestExecutor;

import javax.annotation.Nullable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public final class HttpNode extends AbstractMessageControlNode {

    private final HttpClient httpClient;

    // FIXME
    private SendStrategy sendStrategy = SendStrategy.INDEFINITE;

    volatile boolean active = true;

    // FIXME pull version from build
    private final String userAgent = "Nextop/0.1.3";



    public HttpNode() {
        httpClient = new DefaultHttpClient();
                //HttpClients.createDefault();
    }


    @Override
    public void onActive(boolean active, MessageControlMetrics metrics) {
        this.active = active;

        if (!active) {
            // FIXME on false, upstream.onTransfer(mcs)
        }
    }

    @Override
    public void onTransfer(MessageControlState mcs) {
        super.onTransfer(mcs);

        if (active) {
            // FIXME can have multiple loopers here because mcs does the correct ordering
            // FIXME parameterize
//            for (int i = 0; i < 8; ++i) {
                new Thread(new RequestLooper()).start();
//            }
        }
    }

    @Override
    public void onMessageControl(MessageControl mc) {
        assert MessageControl.Direction.SEND.equals(mc.dir);
        if (active && !mcs.onActiveMessageControl(mc, upstream)) {
            switch (mc.type) {
                case MESSAGE:
                    mcs.add(mc.message);
                    break;
                default:
                    // ignore
                    break;
            }
        }
    }





    private final class RequestLooper implements Runnable {

        @Override
        public void run() {
            while (active) {
                try {
                    @Nullable MessageControlState.Entry entry = mcs.takeFirstAvailable(HttpNode.this,
                            Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
                    if (null != entry) {
                        try {
                            execute(entry.message, entry);
                        } finally {
                            // FIXME ERROR, COMPLETED
                            mcs.remove(entry.id, MessageControlState.End.COMPLETED);
                        }
                    }
                } catch (InterruptedException e) {
                    // continuex
                }
            }

        }

        private void execute(final Message requestMessage, MessageControlState.Entry entry) {
            // FIXME do progress updates



            final HttpUriRequest request;
            try {
                request = Message.toHttpRequest(requestMessage);
            } catch (Exception e) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        upstream.onMessageControl(MessageControl.receive(MessageControl.Type.ERROR, requestMessage));
                    }
                });
                return;
            }

            // FIXME 0.1.1
            // FIXME   surface progress
            // FIXME   retry on transfer up and down if idempotent (resend)
            // FIXME   use jarjar on latest httpclient to have a stable version to work with on android

            // FIXME can do retry here while active {  use supplied retry strategy
            final HttpResponse response;
            try {
                response = httpClient.execute(request);
            } catch (Exception e) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        upstream.onMessageControl(MessageControl.receive(MessageControl.Type.ERROR, requestMessage));
                    }
                });
                return;
            }
            // FIXME }


            // FIXME can't do retry here if not idempotent
            // FIXME *can do* retry here on idempotent (GET, HEAD)
            final Message responseMessage;
            try {
                responseMessage = Message.fromHttpResponse(response).setRoute(requestMessage.inboxRoute()).build();
            } catch (Exception e) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        upstream.onMessageControl(MessageControl.receive(MessageControl.Type.ERROR, requestMessage.inboxRoute()));
                    }
                });
                return;
            }

            // FIXME parse codes
            post(new Runnable() {
                @Override
                public void run() {
                    upstream.onMessageControl(MessageControl.receive(responseMessage));
                    upstream.onMessageControl(MessageControl.receive(MessageControl.Type.COMPLETE, responseMessage.route));
                }
            });
        }

    }




    // PoolingHttpClientConnectionManager
    // -- uses ManagedHttpClientConnectionFactory
    //    -- uses LoggingManagedHttpClientConnection    #getOutputStream(socket)  #getInputStream(Socket)
    //       -- implements ManagedHttpClientConnection     #bind(Socket)
    //       -- extends DefaultManagedHttpClientConnection   WANT TO USE THIS
    // DefaultConnectionReuseStrategy

    // FIXME wrap in a retry exec  with NextopHttpRequestRetryHandler
    // FIXME    use the message property idempotent to influence retry also
    // DefaultHttpRequestRetryHandler

    // implement a subclass of HttpRequestExector that surfaces SendIOException(final chunk), ReceiveIOException
    // implement a custom RetryHandler that always retries if send failed on not final chunk,
    final class NextopClientExec implements ClientExecChain {

        private final HttpRequestExecutor requestExecutor;
        private final HttpClientConnectionManager connManager;
        private final ConnectionReuseStrategy reuseStrategy;
        private final ConnectionKeepAliveStrategy keepAliveStrategy;
        private final HttpProcessor httpProcessor;

        public NextopClientExec(
                final HttpRequestExecutor requestExecutor,
                final HttpClientConnectionManager connManager,
                final ConnectionReuseStrategy reuseStrategy,
                final ConnectionKeepAliveStrategy keepAliveStrategy) {
            Args.notNull(requestExecutor, "HTTP request executor");
            Args.notNull(connManager, "Client connection manager");
            Args.notNull(reuseStrategy, "Connection reuse strategy");
            Args.notNull(keepAliveStrategy, "Connection keep alive strategy");
            this.httpProcessor = new ImmutableHttpProcessor(
                    new RequestContent(),
                    new RequestTargetHost(),
                    new RequestClientConnControl(),
                    new RequestUserAgent(VersionInfo.getUserAgent(
                            "Apache-HttpClient", "org.apache.http.client", getClass())));
            this.requestExecutor    = requestExecutor;
            this.connManager        = connManager;
            this.reuseStrategy      = reuseStrategy;
            this.keepAliveStrategy  = keepAliveStrategy;
        }

        static void rewriteRequestURI(
                final HttpRequestWrapper request,
                final HttpRoute route) throws ProtocolException {
            try {
                URI uri = request.getURI();
                if (uri != null) {
                    // Make sure the request URI is relative
                    if (uri.isAbsolute()) {
                        uri = URIUtils.rewriteURI(uri, null, true);
                    } else {
                        uri = URIUtils.rewriteURI(uri);
                    }
                    request.setURI(uri);
                }
            } catch (final URISyntaxException ex) {
                throw new ProtocolException("Invalid URI: " + request.getRequestLine().getUri(), ex);
            }
        }

        @Override
        public CloseableHttpResponse execute(
                final HttpRoute route,
                final HttpRequestWrapper request,
                final HttpClientContext context,
                final HttpExecutionAware execAware) throws IOException, HttpException {
            Args.notNull(route, "HTTP route");
            Args.notNull(request, "HTTP request");
            Args.notNull(context, "HTTP context");

            rewriteRequestURI(request, route);

            final ConnectionRequest connRequest = connManager.requestConnection(route, null);
            if (execAware != null) {
                if (execAware.isAborted()) {
                    connRequest.cancel();
                    throw new RequestAbortedException("Request aborted");
                } else {
                    execAware.setCancellable(connRequest);
                }
            }

            final RequestConfig config = context.getRequestConfig();

            final HttpClientConnection managedConn;
            try {
                final int timeout = config.getConnectionRequestTimeout();
                managedConn = connRequest.get(timeout > 0 ? timeout : 0, TimeUnit.MILLISECONDS);
            } catch(final InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new RequestAbortedException("Request aborted", interrupted);
            } catch(final ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (cause == null) {
                    cause = ex;
                }
                throw new RequestAbortedException("Request execution failed", cause);
            }

            final ConnectionHolder releaseTrigger = new ConnectionHolder(log, connManager, managedConn);
            try {
                if (execAware != null) {
                    if (execAware.isAborted()) {
                        releaseTrigger.close();
                        throw new RequestAbortedException("Request aborted");
                    } else {
                        execAware.setCancellable(releaseTrigger);
                    }
                }

                if (!managedConn.isOpen()) {
                    final int timeout = config.getConnectTimeout();
                    this.connManager.connect(
                            managedConn,
                            route,
                            timeout > 0 ? timeout : 0,
                            context);
                    this.connManager.routeComplete(managedConn, route, context);
                }
                final int timeout = config.getSocketTimeout();
                if (timeout >= 0) {
                    managedConn.setSocketTimeout(timeout);
                }


                // FIXME managedConn is an instanceof of ProgressHttpClientConnection
                // FIXME attach the progresscallback if exists

                HttpHost target = null;
                final HttpRequest original = request.getOriginal();
                if (original instanceof HttpUriRequest) {
                    final URI uri = ((HttpUriRequest) original).getURI();
                    if (uri.isAbsolute()) {
                        target = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
                    }
                }
                if (target == null) {
                    target = route.getTargetHost();
                }

                context.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, target);
                context.setAttribute(HttpCoreContext.HTTP_REQUEST, request);
                context.setAttribute(HttpCoreContext.HTTP_CONNECTION, managedConn);
                context.setAttribute(HttpClientContext.HTTP_ROUTE, route);

                httpProcessor.process(request, context);
                final HttpResponse response = requestExecutor.execute(request, managedConn, context);
                httpProcessor.process(response, context);

                // The connection is in or can be brought to a re-usable state.
                if (reuseStrategy.keepAlive(response, context)) {
                    // Set the idle duration of this connection
                    final long duration = keepAliveStrategy.getKeepAliveDuration(response, context);
                    releaseTrigger.setValidFor(duration, TimeUnit.MILLISECONDS);
                    releaseTrigger.markReusable();
                } else {
                    releaseTrigger.markNonReusable();
                }

                // check for entity, release connection if possible
                final HttpEntity entity = response.getEntity();
                if (entity == null || !entity.isStreaming()) {
                    // connection not needed and (assumed to be) in re-usable state
                    releaseTrigger.releaseConnection();
                    return new HttpResponseProxy(response, null);
                } else {
                    return new HttpResponseProxy(response, releaseTrigger);
                }
            } catch (final ConnectionShutdownException ex) {
                final InterruptedIOException ioex = new InterruptedIOException(
                        "Connection has been shut down");
                ioex.initCause(ex);
                throw ioex;
            } catch (final HttpException ex) {
                releaseTrigger.abortConnection();
                throw ex;
            } catch (final IOException ex) {
                releaseTrigger.abortConnection();
                throw ex;
            } catch (final RuntimeException ex) {
                releaseTrigger.abortConnection();
                throw ex;
            }
        }

    }


    // be able to reset progress
    // be able to attach callback that gets called after A bytes of upload, B bytes of download indiviudally
    static final class ProgressHttpClientConnection extends DefaultManagedHttpClientConnection {

        // OVERRIDE sendRequestEntity
        // throw a SendIO

        // FIXME if TCP error on close, throw SendIOException
        // FIXME this means all packets sent up to the tcp window size,
        // FIXME but failed to ack the end
        // FIXME otherwise, up to the end of the entity was not sent, so the server knows it has a hanging request
        @Override
        protected OutputStream prepareOutput(HttpMessage message) throws HttpException {
            return new FilterOutputStream(super.prepareOutput(message)) {
                @Override
                public void close() throws IOException {
                    // this blocks until the tcp window is drained
                    // if all acks don't come back, then the server may or may not have received all the data
                    try {
                        super.close();
                    } catch (IOException e) {
                        throw new SendIOException(e, true);
                    }
                }
            };
        }
    }


    static class NextopHttpRequestExecutor extends HttpRequestExecutor {


        @Override
        protected HttpResponse doSendRequest(
                final HttpRequest request,
                final HttpClientConnection conn,
                final HttpContext context) throws IOException, HttpException {

            try {
                return super.doSendRequest(request, conn, context);
            } catch (IOException e) {
                if (e instanceof SendIOException) {
                    throw e;
                }
                throw new SendIOException(e);
            } catch (HttpException e) {
                if (e instanceof SendHttpException) {
                    throw e;
                }
                throw new SendHttpException(e);
            }
        }

        @Override
        protected HttpResponse doReceiveResponse(
                final HttpRequest request,
                final HttpClientConnection conn,
                final HttpContext context) throws HttpException, IOException {
            try {
                return super.doReceiveResponse(request, conn, context);
            } catch (IOException e) {
                if (e instanceof ReceiveIOException) {
                    throw e;
                }
                throw new ReceiveIOException(e);
            } catch (HttpException e) {
                if (e instanceof ReceiveHttpException) {
                    throw e;
                }
                throw new ReceiveHttpException(e);
            }
        }


    }



}
