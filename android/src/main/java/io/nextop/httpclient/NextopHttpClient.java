package io.nextop.httpclient;

import io.nextop.Message;
import io.nextop.Nextop;
import io.nextop.NextopAndroid;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import rx.functions.Func1;

import javax.annotation.Nullable;
import java.io.IOException;

// FIXME(compat)
public class NextopHttpClient implements HttpClient {

    private final Nextop nextop;
    private final HttpParams params;
    private final ClientConnectionManager connectionManager;


    public NextopHttpClient(@Nullable ClientConnectionManager connectionManager,
                            @Nullable HttpParams params) {
        this(connectionManager, params, NextopAndroid.getActive());
    }

    public NextopHttpClient(@Nullable ClientConnectionManager connectionManager,
                            @Nullable HttpParams params,
                            Nextop nextop) {
        this.params = params;
        this.connectionManager = connectionManager;

        this.nextop = nextop;
    }


    @Override
    public HttpParams getParams() {
        // important: this object is currently benign
        // TODO attempt to map values set here into the Nextop messages
        return params;
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        // important: this object is currently benign
        // TODO attempt to map values set here into the Nextop messages
        return connectionManager;
    }



    @Override
    public HttpResponse execute(@Nullable HttpHost httpHost, HttpRequest httpRequest, @Nullable HttpContext httpContext) throws IOException, ClientProtocolException {
        // TODO httpContext? currently ignored

        Message message = fromHttpRequest(httpHost, httpRequest);
        // FIXME attach cancel policy on request to nextop.cancelSend(id)
        return execute(nextop, message);
    }

    @Override
    public <T> T execute(@Nullable HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler, @Nullable HttpContext httpContext) throws IOException, ClientProtocolException {
        HttpResponse response = execute(httpHost, httpRequest, httpContext);
        return responseHandler.handleResponse(response);
    }


    @Override
    public HttpResponse execute(HttpUriRequest httpUriRequest) throws IOException, ClientProtocolException {
        return execute((HttpHost) null, httpUriRequest, (HttpContext) null);
    }

    @Override
    public HttpResponse execute(HttpUriRequest httpUriRequest, HttpContext httpContext) throws IOException, ClientProtocolException {
        return execute((HttpHost) null, httpUriRequest, httpContext);
    }

    @Override
    public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest) throws IOException, ClientProtocolException {
        return execute(httpHost, httpRequest, (HttpContext) null);
    }

    @Override
    public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return execute((HttpHost) null, httpUriRequest, responseHandler, (HttpContext) null);
    }

    @Override
    public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler, HttpContext httpContext) throws IOException, ClientProtocolException {
        return execute((HttpHost) null, httpUriRequest, responseHandler, httpContext);
    }

    @Override
    public <T> T execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return execute(httpHost, httpRequest, responseHandler, (HttpContext) null);
    }



    public static HttpResponse execute(Nextop nextop, Message message) throws IOException {
        Nextop.Receiver<Message> receiver = nextop.send(message);

        Message responseMessage = receiver.onErrorReturn(new Func1<Throwable, Message>() {
            @Override
            public Message call(Throwable throwable) {
                // TODO handle the error more precisely
                return Message.newBuilder().setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
            }
        }).defaultIfEmpty(
                // if using the nextop protocol, some successful messages don't have a response code,
                // just an ack which closes the channel
                Message.newBuilder().setCode(HttpStatus.SC_OK).build()
        ).toBlocking().single();

        return toHttpResponse(responseMessage);
    }



    public static Message fromHttpRequest(HttpHost httpHost, HttpRequest httpRequest) {
        return fromHttpRequestBuilder(httpHost, httpRequest).build();
    }

    public static Message.Builder fromHttpRequestBuilder(HttpHost httpHost, HttpRequest httpRequest) {
        // FIXME support request.abort

        // FIXME adapt org.apache.* to io.nextop.org.apache.* then use Message.fromHttpRequest,
        // FIXME and adapt back with abort behavior mixed in

        return null;
    }

    public static HttpResponse toHttpResponse(Message message) {
        // FIXME use Message.toHttpResponse and adapt back with abort behavior mixed in

        return null;
    }
}
