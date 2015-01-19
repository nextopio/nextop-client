package io.nextop.client;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.nextop.Message;
import io.nextop.WireValue;
import io.nextop.util.HexBytes;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// FIXME ordering and retry
public class HttpNode extends AbstractMessageControlNode {

    private Executor executor;
    private CloseableHttpClient httpClient;


    public HttpNode(Wire.Factory wireFactory) {
        this(wireFactory, Executors.newCachedThreadPool());
    }

    public HttpNode(Wire.Factory wireFactory, Executor executor) {

        this.executor = executor;
        httpClient = HttpClients.createDefault();
    }


    @Override
    public void onActive(boolean active, MessageControlMetrics metrics) {
        // FIXME on false, upstream.onTransfer(mcs)
    }

    @Override
    public void onTransfer(MessageControlState mcs) {
        // FIXME
    }


    @Override
    public void onMessageControl(MessageControl mc) {
        switch (mc.type) {
            case SEND:
                // FIXME update the mcs?
                onSend(mc.message);
                break;

            case SUBSCRIBE:
                // FIXME put this in the mcs
                break;

            case UNSUBSCRIBE:
                // FIXME put this in the mcs
                break;

            case SEND_NACK:
                // FIXME update the mcs
                break;

            case RECEIVE_ACK:
                // FIXME update the mcs
                break;

            case RECEIVE_NACK:
                // FIXME update the mcs
                break;
        }
    }



    private void onSend(Message message) {
        executor.execute(new RequestWorker(message));
    }

    private void onSendError(Message message) {
        upstream.onMessageControl(new MessageControl(MessageControl.Type.SEND_ERROR, message));
    }


    private void onReceive(Message message) {
        upstream.onMessageControl(new MessageControl(MessageControl.Type.RECEIVE, message));

    }

    private void onReceiveError(Message message) {
        upstream.onMessageControl(new MessageControl(MessageControl.Type.RECEIVE_ERROR, message));

    }



    private final class RequestWorker implements Runnable {
        final Message requestMessage;

        RequestWorker(Message requestMessage) {
            this.requestMessage = requestMessage;
        }

        @Override
        public void run() {
            final HttpUriRequest request;
            try {
                request = Message.toHttpRequest(requestMessage);
            } catch (Exception e) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        onSendError(requestMessage);
                    }
                });
                return;
            }

            final CloseableHttpResponse response;
            try {
                response = httpClient.execute(request);
            } catch (Exception e) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        onSendError(requestMessage);
                    }
                });
                return;
            }

            final Message responseMessage;
            try {
                responseMessage = Message.fromHttpResponse(response).setNurl(requestMessage.receiverNurl()).build();
            } catch (Exception e) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        onReceiveError(Message.newBuilder().setNurl(requestMessage.receiverNurl()).build());
                    }
                });
                return;
            }

            post(new Runnable() {
                @Override
                public void run() {
                    onReceive(responseMessage);
                }
            });
        }
    }



}
