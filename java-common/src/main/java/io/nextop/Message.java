package io.nextop;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import rx.functions.Func1;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;

import static io.nextop.Route.Method;
import static io.nextop.Route.Target;

// goals: fast to parse, minimal object creation on parse or each operation
public class Message {
    public static final WireValue P_CONTENT = WireValue.of("$content");
    public static final WireValue P_FRAGMENT = WireValue.of("$fragment");


    public static final Id DEFAULT_GROUP_ID = Id.create(0L, 0L, 0L, 0L);
    public static final int DEFAULT_GROUP_PRIORITY = 0;


    public static Route outboxRoute(Id id) {
        return Route.local(Target.create(Method.PUT, Path.valueOf(id.toString())));
    }
    /** @see Route#getLocalId */
    public static Route inboxRoute(Id id) {
        return Route.local(Target.create(Method.POST, Path.valueOf(id.toString())));
    }
    /** @see Route#getLocalId */
    public static Route echoRoute(Id id) {
        return Route.local(Target.create(Method.GET, Path.valueOf("/" + id)));
    }
    /** @see Route#getLocalId */
    public static Route echoHeadRoute(Id id) {
        return Route.local(Target.create(Method.HEAD, Path.valueOf("/" + id)));
    }
    /** @see Route#getLocalId */
    public static Route statusRoute(Id id) {
        return Route.local(Target.create(Method.GET, Path.valueOf("/" + id + "/status")));
    }


    public static final WireValue P_PROGRESS = WireValue.of("progress");



    public static Message valueOf(Route.Method method, URL url) {
        try {
            return valueOf(method, url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }


    public static Message valueOf(Route.Method method, URI uri) {
        // FIXME parse query and fragment into parameters
        return newBuilder().setRoute(Route.valueOf(String.format("%s %s", method, uri))).build();
    }



    public final Id id;
    // priority sets the priority of the group relative to other groups
    // groups are equal based on *id only*
    // on the send/receive side, when multiplexing, priority of a group should be the max of all queued messages
    public final Id groupId;
    public final int groupPriority;
    public final Route route;
    public final Map<WireValue, WireValue> headers;
    public final Map<WireValue, WireValue> parameters;


    Message(Id id, Id groupId, int groupPriority, Route route,
                    Map<WireValue, WireValue> headers,
                    Map<WireValue, WireValue> parameters) {
        this.id = id;
        this.groupId = groupId;
        this.groupPriority = groupPriority;
        this.route = route;
        this.headers = headers;
        this.parameters = parameters;
    }




    // ROUTES

    public Route outboxRoute() {
        return outboxRoute(id);
    }
    public Route inboxRoute() {
        return inboxRoute(id);
    }
    public Route echoRoute() {
        return echoRoute(id);
    }
    public Route echoHeadRoute() {
        return echoHeadRoute(id);
    }
    public Route statusRoute() {
        return statusRoute(id);
    }







    @Nullable
    public WireValue getContent() {
        return parameters.get(P_CONTENT);
    }



    public URI toUri() throws URISyntaxException {
        return toUri(this);
    }

    public HttpUriRequest toHttpRequest() throws URISyntaxException {
        return toHttpRequest(this);
    }




    public Builder toBuilder() {
        Builder b = newBuilder()
                .setGroupId(groupId)
                .setGroupPriority(groupPriority)
                .setRoute(route);

        for (Map.Entry<WireValue, WireValue> e : headers.entrySet()) {
            b = b.setHeader(e.getKey(), e.getValue());
        }
        for (Map.Entry<WireValue, WireValue> e : parameters.entrySet()) {
            b = b.set(e.getKey(), e.getValue());
        }

        return b;
    }


    public Message newId() {
        return toBuilder().build();
    }

    public Message parallel() {
        return toBuilder().setGroupId(Id.create()).build();
    }

    public Message serial(Id serialGroupId) {
        return toBuilder().setGroupId(serialGroupId).build();
    }



    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private final Id id = Id.create();

        private Id groupId = DEFAULT_GROUP_ID;
        private int groupPriority = DEFAULT_GROUP_PRIORITY;
        @Nullable
        private Route.Target target = null;
        @Nullable
        private Route.Via via = null;

        private Map<WireValue, WireValue> headers = new HashMap<WireValue, WireValue>(8);
        private Map<WireValue, WireValue> parameters = new HashMap<WireValue, WireValue>(8);


        private Builder() {
        }


        public Builder setGroupId(Id groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder setGroupPriority(int groupPriority) {
            this.groupPriority = groupPriority;
            return this;
        }

        public Builder setTarget(@Nullable Route.Target target) {
            this.target = target;
            return this;
        }
        public Builder setTarget(@Nullable String target) {
            if (null != target) {
                this.target = Route.Target.valueOf(target);
            } else {
                this.target = null;
            }
            return this;
        }

        public Builder setVia(@Nullable Route.Via via) {
            this.via = via;
            return this;
        }
        public Builder setVia(@Nullable String via) {
            if (null != via) {
                this.via = Route.Via.valueOf(via);
            } else {
                this.via = null;
            }
            return this;
        }

        public Builder setRoute(@Nullable Route route) {
            if (null != route) {
                this.target = route.target;
                this.via = route.via;
            } else {
                this.target = null;
                this.via = null;
            }
            return this;
        }
        public Builder setRoute(@Nullable String s) {
            if (null != s) {
                Route route = Route.valueOf(s);
                target = route.target;
                via = route.via;
            } else {
                target = null;
                via = null;
            }
            return this;
        }


        public Builder setHeader(Object name, @Nullable Object value) {
            if (null != value) {
                headers.put(WireValue.of(name), WireValue.of(value));
            } else {
                headers.remove(WireValue.of(name));
            }
            return this;
        }

        public Builder setContent(@Nullable Object value) {
            return set(P_CONTENT, value);
        }

        public Builder set(Object name, @Nullable Object value) {
            if (null != value) {
                parameters.put(WireValue.of(name), WireValue.of(value));
            } else {
                parameters.remove(WireValue.of(name));
            }
            return this;
        }



        public Message build() {
            if (null == target) {
                throw new IllegalStateException();
            }
            Route route;
            if (null != via) {
                route = Route.create(target, via);
            } else {
                route = Route.local(target);
            }
            return new Message(id, groupId, groupPriority, route,
                    ImmutableMap.copyOf(headers),
                    ImmutableMap.copyOf(parameters));
        }




    }


    /////// URI CONVERSIONS ///////

    /** @throws java.net.URISyntaxException if variables in the path cannot be fully substituted. */
    public static URI toUri(final Message message) throws URISyntaxException {
        // variables in path get substituted to path;
        // if not all variables can be substituted, throw a URISyntaxException
        // parameters except P_CONTENT get added as query params (via toString)

        Path fixedPath = message.route.target.path.fix(new Func1<String, Object>() {
            @Override
            public Object call(String s) {
                return message.parameters.get(WireValue.of(s));
            }
        });

        URIBuilder builder = new URIBuilder();
        builder.setScheme(message.route.via.scheme.toString());
        builder.setHost(message.route.via.authority.getHost());
        if (0 < message.route.via.authority.port) {
            builder.setPort(message.route.via.authority.port);
        }
        builder.setPath(fixedPath.toString());

        for (Map.Entry<WireValue, WireValue> e : message.parameters.entrySet()) {
            WireValue key = e.getKey();
            WireValue value = e.getValue();
            if (!P_CONTENT.equals(key)) {
                builder.addParameter(key.toString(), value.toString());
            }
        }

        return builder.build();
    }


    /////// HTTPCLIENT CONVERSIONS ///////

    private static final String H_PRAGMA_ID_PREFIX = "nextop-id";
    // FIXME serialID
    private static final String H_PRAGMA_PREFIX = "nextop-header";

    // IMAGE
    // FIXME maxTransferWidth, maxTransferHeight



    public static HttpUriRequest toHttpRequest(Message message) throws URISyntaxException {
        switch (message.route.target.method) {
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

    // requires a route set into the builder to build
    public static Builder fromHttpResponse(HttpResponse response) {

        Message.Builder builder = Message.newBuilder();

        attachHeaders(response, builder);
        attachContent(response, builder);

        return builder;
    }

    /** inverse of {@link #toHttpRequest} */
    public static Message fromHttpRequest(HttpUriRequest request) {
        // FIXME
        return null;
    }

    /** inverse of {@link #fromHttpResponse} */
    public static HttpResponse toHttpResponse(Message message) {
        // FIXME
        return null;
    }




    private static void attachHeaders(Message message, HttpRequest request) {
        // add the id
        request.addHeader(HttpHeaders.PRAGMA, String.format("%s %s",
                H_PRAGMA_ID_PREFIX, message.id));

        for (Map.Entry<WireValue, WireValue> e : message.headers.entrySet()) {
            WireValue key = e.getKey();
            WireValue value = e.getValue();
            String keyString = key.toString();
            if (ALL_HTTP_HEADERS.contains(keyString)) {
                request.setHeader(keyString, value.toText());
            } else {
                // send non-standard headers as Pragma headers:
                // $H_PRAGMA_PREFIX $json([$key, $value])
                request.addHeader(HttpHeaders.PRAGMA, String.format("%s %s",
                        H_PRAGMA_PREFIX, WireValue.of(Arrays.asList(key, value)).toJson()));
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
                entity = new StringEntity(content.toJson());
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
                value = WireValue.valueOfJson(new InputStreamReader(entity.getContent(), Charsets.UTF_8));
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




    private static final Set<String> ALL_HTTP_HEADERS;
    static {
        // TODO fix this to not use reflection (be explicit; easier to be cross platform)
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
        ALL_HTTP_HEADERS = ImmutableSet.copyOf(httpHeaders);
    }
}
