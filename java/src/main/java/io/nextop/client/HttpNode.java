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
                request = createRequest(requestMessage);
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
                responseMessage = createResponse(requestMessage, response);
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



    private static HttpUriRequest createRequest(Message message) throws URISyntaxException {
        switch (message.nurl.target.method) {
            case GET: {
                HttpGet get = new HttpGet();
                get.setURI(message.toUri());
                attachHeaders(message, get);
                return get;
            }
            case POST: {
                HttpPost post = new HttpPost();
                post.setURI(message.toUri());
                attachHeaders(message, post);
                attachContent(message, post);
                return post;
            }
            default:
                throw new IllegalArgumentException();
        }
    }

    private static final String PRAGMA_PREFIX_HEADER = "nextop-header";
//    private static final String PRAGMA_PREFIX_PARAMETER = "nextop-parameter";


    private static void attachHeaders(Message message, HttpRequest request) {
        // send non-standard headers as Pragma headers:
        // nextop-header [key, value]
        for (Map.Entry<WireValue, WireValue> e : message.headers.entrySet()) {
            WireValue key = e.getKey();
            WireValue value = e.getValue();
            String keyString = key.toString();
            if (HTTP_HEADERS.contains(keyString)) {
                request.setHeader(keyString, value.toString());
            } else {
                request.addHeader(HttpHeaders.PRAGMA, String.format("%s %s",
                        PRAGMA_PREFIX_HEADER, WireValue.of(Arrays.asList(key, value)).toJsonString()));
            }
        }
    }

    private static void attachContent(Message message, HttpEntityEnclosingRequestBase request) {
        WireValue content = message.parameters.get(Message.P_CONTENT);
        if (null != content) {
            HttpEntity entity = createEntity(message, content);
            assert entity.isRepeatable();
            request.setEntity(entity);

            // TODO
//            if ("http".equalsIgnoreCase(request.getURI().getScheme())
//                    && !message.headers.containsKey(WireValue.of(HttpHeaders.CONTENT_MD5))) {
//                request.setHeader(HttpHeaders.CONTENT_MD5, WireValue.of(contentMd5(entity)).toString());
//            } // else https already checks integrity of message
        }
    }



    private static HttpEntity createEntity(Message message, WireValue content) {
        HttpEntity entity;
        MediaType contentType = getContentType(message, content);
        if (contentType.is(MediaType.JSON_UTF_8)) {
            try {
                entity = new StringEntity(content.toJsonString());
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e);
            }
        } else if (contentType.is(MediaType.ANY_TEXT_TYPE)) {
            try {
                entity = new StringEntity(content.toString());
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            // send as binary
            ByteBuffer bb = content.asBlob();
            byte[] bytes = new byte[bb.remaining()];
            bb.get(bytes);
            entity = new ByteArrayEntity(bytes);
        }
        return entity;
    }
    private static MediaType getContentType(Message message, WireValue content) {
        @Nullable WireValue contentTypeValue = message.headers.get(WireValue.of(HttpHeaders.CONTENT_TYPE));
        if (null != contentTypeValue) {
            return MediaType.parse(contentTypeValue.asString());
        }
        // else use a default content type
        switch (content.getType()) {
            case BLOB:
                return MediaType.APPLICATION_BINARY;
            case UTF8:
                return MediaType.PLAIN_TEXT_UTF_8;
            default:
                return MediaType.JSON_UTF_8;
        }
    }





    private static Message createResponse(Message requestMessage, HttpResponse response) {
        // FIXME

        // 1. nurl of response is request receiveNurl
        // 2. convert content to wirevalue (e.g. json gets parsed and converted)
        // 3. convert headers

        Message.Builder builder = Message.newBuilder();

        builder.setNurl(requestMessage.receiverNurl());

        attachHeaders(response, builder);
        attachContent(response, builder);

        return builder.build();
    }


    private static void attachHeaders(HttpResponse response, Message.Builder builder) {
        for (Header header : response.getAllHeaders()) {
            builder.setHeader(WireValue.of(header.getName()), WireValue.of(header.getValue()));
        }
    }
    private static void attachContent(HttpResponse response, Message.Builder builder) {
        @Nullable HttpEntity entity = response.getEntity();
        if (null != entity) {
            builder.setContent(createContent(response, entity));
        }
    }

    private static WireValue createContent(HttpResponse response, HttpEntity entity) {
        // 1. if there is a Content-MD5 header, check the MD5
        // 2. parse based on the content type


        // TODO this is a transmission error then, but there is no way to retransmit ...
        // TODO this should be surfaced as a receive error and the receiver() can do the logical thing ...
        // TODO maybe have a header H_IDEMPOTENT or something, where receive errors can be regenerated by re-sending
//        for (Header header : response.getHeaders(HttpHeaders.CONTENT_MD5)) {
//            byte[] headerMd5 = Base64.decodeBase64(header.getValue());
//            byte[] contentMd5 = contentMd5(entity);
//            if (!Arrays.equals(headerMd5, contentMd5)) {
//                throw new IllegalArgumentException(String.format("%s %s does not match %s", HttpHeaders.CONTENT_MD5,
//                        HexBytes.toString(headerMd5), HexBytes.toString(contentMd5)));
//            }
//        }


        WireValue value;
        MediaType contentType = MediaType.parse(entity.getContentType().getValue());
        if (contentType.is(MediaType.JSON_UTF_8)) {
            try {
                JsonReader r = new JsonReader(new InputStreamReader(entity.getContent(), Charsets.UTF_8));
                try {
                    value = parseJson(r);
                    assert JsonToken.END_DOCUMENT.equals(r.peek());
                } finally {
                    r.close();
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        } else if (contentType.is(MediaType.ANY_TEXT_TYPE)) {
            try {
                Reader r = new InputStreamReader(entity.getContent(), Charsets.UTF_8);
                try {
                    value = WireValue.of(CharStreams.toString(r));
                } finally {
                    r.close();
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            // receive as binary
            try {
                InputStream is = entity.getContent();
                try {
                    value = WireValue.of(ByteStreams.toByteArray(is));
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return value;
    }

    private static WireValue parseJson(JsonReader r) throws IOException {
        switch (r.peek()) {
            case BEGIN_OBJECT: {
                Map<WireValue, WireValue> map = new HashMap<WireValue, WireValue>(4);
                r.beginObject();
                    while (!JsonToken.END_OBJECT.equals(r.peek())) {
                        WireValue key = WireValue.of(r.nextName());
                        WireValue value = parseJson(r);
                        map.put(key, value);
                    }
                r.endObject();
                return WireValue.of(map);
            }
            case BEGIN_ARRAY: {
                List<WireValue> list = new ArrayList<WireValue>(4);
                r.beginArray();
                while (!JsonToken.END_ARRAY.equals(r.peek())) {
                    WireValue value = parseJson(r);
                    list.add(value);
                }
                r.endArray();
                return WireValue.of(list);
            }
            case STRING:
                return WireValue.of(r.nextString());
            case NUMBER: {
                try {
                    long n = r.nextLong();
                    if ((int) n == n) {
                        return WireValue.of((int) n);
                    } else {
                        return WireValue.of(n);
                    }
                } catch (NumberFormatException e) {
                    double d = r.nextDouble();
                    if ((float) d == d) {
                        return WireValue.of((float) d);
                    } else {
                        return WireValue.of(d);
                    }
                }
            }
            case BOOLEAN:
                return WireValue.of(r.nextBoolean());
            case NULL:
                // FIXME have a NULL WireValue Type
                throw new IllegalArgumentException();
            default:
            case END_DOCUMENT:
                throw new IllegalArgumentException();
        }
    }



//    private static byte[] contentMd5(HttpEntity entity) {
//        try {
//            InputStream is = entity.getContent();
//            try {
//                return DigestUtils.md5(is);
//            } finally {
//                is.close();
//            }
//        } catch (IOException e) {
//            throw new IllegalArgumentException(e);
//        }
//    }


    //

    private static final Set<String> HTTP_HEADERS;
    static {
        Set<String> httpHeaders = new HashSet<String>(256);
        for (Field f : HttpHeaders.class.getFields()) {
            int m = f.getModifiers();
            try {
                if (Modifier.isPublic(m) && Modifier.isStatic(m)) {
                    httpHeaders.add(f.get(null).toString());
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        HTTP_HEADERS = ImmutableSet.copyOf(httpHeaders);
    }
}
