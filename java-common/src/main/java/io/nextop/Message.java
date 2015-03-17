package io.nextop;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import io.nextop.org.apache.http.*;
import io.nextop.org.apache.http.client.methods.*;
import io.nextop.org.apache.http.client.utils.URIBuilder;
import io.nextop.org.apache.http.entity.ByteArrayEntity;
import io.nextop.org.apache.http.entity.StringEntity;
import io.nextop.util.NoCopyByteArrayOutputStream;
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
// FIXME implement hashCode, equals
public class Message {
    public static final WireValue P_CONTENT = WireValue.of("$content");
    public static final WireValue P_FRAGMENT = WireValue.of("$fragment");
    public static final WireValue P_CODE = WireValue.of("$code");
    public static final WireValue P_REASON = WireValue.of("$reason");

    /** if the message is redirected, this holds the route (list[string])
     * that the message was redirected from. */
    public static final WireValue H_REDIRECT = WireValue.of("$redirect");
    /** @see #isIdempotent */
    public static final WireValue H_IDEMPOTENT = WireValue.of("$idempotent");
    /** @see #isNullipotent */
    public static final WireValue H_NULLIPOTENT = WireValue.of("$nullopotent");
    /** @see #isYieldable */
    public static final WireValue H_YIELDABLE = WireValue.of("$yieldable");


    // PASSIVE MODES
    // FIXME(0.2) important for battery - support this in all nodes
    /* passive options control the scheduling of the message.
     * by default (no passive) messages are sent with best effort.
     * Passive options can be used to limit battery usage in apps
     * that send a continuous stream of data, like logs.
     * Passing options are:
      * - {@link #V_PASSIVE_ACTIVE_RADIO} */
    public static final WireValue H_PASSIVE = WireValue.of("$passive");
    /** send the message be sent when the radio is active,
     * but do not activate the radio to send it.
     * Implementations can use a heuristic for an active radio:
     * &le;30s after a best effort message is sent. */
    public static final int V_PASSIVE_HOLD_FOR_ACTIVE_RADIO = 1;


    public static final Id DEFAULT_GROUP_ID = Id.create(0L, 0L, 0L, 0L);
    public static final int DEFAULT_GROUP_PRIORITY = 10;

    public static final Id LOG_GROUP_ID = Id.create(0L, 0L, 0L, 1L);
    public static final int LOG_GROUP_PRIORITY = 0;



    /////// ROUTES ///////

    private static final Path P_MESSAGE_PREFIX = Path.valueOf("/m");

    /** @see #getLocalId */
    public static Route outboxRoute(Id id) {
        return Route.local(Target.create(Method.PUT, P_MESSAGE_PREFIX.append(id.toString())));
    }
    /** @see #getLocalId */
    public static Route inboxRoute(Id id) {
        return Route.local(Target.create(Method.POST, P_MESSAGE_PREFIX.append(id.toString())));
    }
    /** @see #getLocalId */
    public static Route echoRoute(Id id) {
        return Route.local(Target.create(Method.GET, P_MESSAGE_PREFIX.append("/" + id)));
    }
    /** @see #getLocalId */
    public static Route echoHeadRoute(Id id) {
        return Route.local(Target.create(Method.HEAD, P_MESSAGE_PREFIX.append("/" + id)));
    }

    public static Route logRoute() {
        return Route.local(Target.create(Method.POST, Path.valueOf("/log")));
    }


    public static boolean isLocal(Route route) {
        return route.via.isLocal();
    }

    @Nullable
    public static Id getLocalId(Route route) {
        if (isLocal(route) && route.target.path.isFixed()
                && route.target.path.startsWith(P_MESSAGE_PREFIX) && 2 <= route.target.path.segments.size()) {
            Path.Segment first = route.target.path.segments.get(0);
            assert Path.Segment.Type.FIXED.equals(first.type);
            try {
                return Id.valueOf(first.value);
            } catch (IllegalArgumentException e) {
                // FIXME log, strange ... could be a client-generated bad value
                return null;
            }
        }
        return null;
    }


    /////// CONTROL PROPERTIES ///////

    /** Test if the message can be safely received multiple times. */
    public static boolean isIdempotent(Message message) {
        @Nullable WireValue idempotentValue = message.headers.get(H_IDEMPOTENT);
        if (null != idempotentValue) {
            return idempotentValue.asBoolean();
        }

        if (isNullipotent(message)) {
            return true;
        }

        // not enough info to infer
        return false;
    }

    /** Tests if the message does not have side effects. */
    public static boolean isNullipotent(Message message) {
        @Nullable WireValue nullipotentValue = message.headers.get(H_NULLIPOTENT);
        if (null != nullipotentValue) {
            return nullipotentValue.asBoolean();
        }

        // not explicitly set; infer
        switch (message.route.target.method) {
            case GET:
            case HEAD:
                return true;
            default:
                return false;
        }
    }

    /** Tests if the message can be moved to the end of the queue on failure.
     * This is one way to solve head of line blocking issues, with failing endpoints,
     * where yieldable requests hitting the failing endpoints will "get in the back of the line"
     * and try again.
     *
     * The default value assumes all nullipotent ({@link #isNullipotent}) requests can
     * yield. This may break some cases where the client needs to get an exact state
     * (e.g. after a certain case), but this can be fixed by either marking the request
     * explicitly yieldable=false, or bundling the request into the response of the certain case. */
    public static boolean isYieldable(Message message) {
        @Nullable WireValue yieldableValue = message.headers.get(H_YIELDABLE);
        if (null != yieldableValue) {
            return yieldableValue.asBoolean();
        }

        // not explicitly set; infer
        return isNullipotent(message);
    }



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
        if (null == id) {
            throw new IllegalArgumentException();
        }
        if (null == groupId) {
            throw new IllegalArgumentException();
        }
        if (null == route) {
            throw new IllegalArgumentException();
        }
        if (null == headers) {
            throw new IllegalArgumentException();
        }
        if (null == parameters) {
            throw new IllegalArgumentException();
        }

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
//    public Route statusRoute() {
//        return statusRoute(id);
//    }







    @Nullable
    public WireValue getContent() {
        return parameters.get(P_CONTENT);
    }

    public int getCode() {
        @Nullable WireValue codeValue = parameters.get(P_CODE);
        return null != codeValue ? codeValue.asInt() : -1;
    }

    @Nullable
    public String getReason() {
        WireValue reasonValue = parameters.get(P_REASON);
        return null != reasonValue ? reasonValue.asString() : null;
    }



    public URI toUri() throws URISyntaxException {
        return toUri(this);
    }

    public String toUriString() {
        try {
            return toUri(this).toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException();
        }
    }



    public Builder buildOn() {
        return toBuilder(false);
    }
    public Builder toBuilder() {
        return toBuilder(true);
    }

    private Builder toBuilder(boolean newId) {
        Builder b = (newId ? newBuilder() : newBuilder(id))
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

    // FIXME this needs to set parameters that are part of the path
//    public Message toSpec() {
//        return newBuilder()
//                .setRoute(route)
//                .build();
//    }


//    public Message newId() {
//        return toBuilder().build();
//    }
//
//    public Message parallel() {
//        return buildOn().setGroupId(Id.create()).build();
//    }
//
//    public Message serial(Id serialGroupId) {
//        return buildOn().setGroupId(serialGroupId).build();
//    }



    public static Builder newBuilder() {
        return newBuilder(Id.create());
    }

    public static Builder newBuilder(Id id) {
        return new Builder(id);
    }



    @Override
    public String toString() {
        try {
            return String.format("%s", toUri());
        } catch (URISyntaxException e) {
            return String.format("<%s>", route);
        }
    }


    @Override
    public int hashCode() {
        int c = id.hashCode();
        c = 31 * c + groupId.hashCode();
        c = 31 * c + groupPriority;
        c = 31 * c + route.hashCode();
        c = 31 * c + headers.hashCode();
        c = 31 * c + parameters.hashCode();
        return c;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Message)) {
            return false;
        }

        Message b = (Message) obj;
        return id.equals(b.id)
                && groupId.equals(b.groupId) && groupPriority == b.groupPriority
                && route.equals(b.route)
                && headers.equals(b.headers)
                && parameters.equals(b.parameters);

//        if (!id.equals(b.id)) {
//            return false;
//        }
//        if (!groupId.equals(b.groupId)) {
//            return false;
//        }
//        if (groupPriority != b.groupPriority) {
//            return false;
//        }
//        if (!route.equals(b.route)) {
//            return false;
//        }
//        if (!headers.equals(b.headers)) {
//            return false;
//        }
//        if (!parameters.equals(b.parameters)) {
//            return false;
//        }
//        return true;
    }


    public static final class Builder {
        private final Id id;

        private Id groupId = DEFAULT_GROUP_ID;
        private int groupPriority = DEFAULT_GROUP_PRIORITY;
        @Nullable
        private Route.Target target = null;
        @Nullable
        private Route.Via via = null;

        private Map<WireValue, WireValue> headers = new HashMap<WireValue, WireValue>(8);
        private Map<WireValue, WireValue> parameters = new HashMap<WireValue, WireValue>(8);


        private Builder(Id id) {
            this.id = id;
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

        public Builder setCode(@Nullable Integer code) {
            return set(P_CODE, code);
        }

        public Builder setReason(@Nullable String reason) {
            return set(P_REASON, reason);
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
        Set<String> pathVariableNames = new HashSet<String>(4);
        for (Path.Segment segment : message.route.target.path.segments) {
            if (Path.Segment.Type.VARIABLE.equals(segment.type)) {
                pathVariableNames.add(segment.value);
            }
        }

        URIBuilder builder = new URIBuilder();
        builder.setScheme(message.route.via.scheme.toString().toLowerCase());
        if (null != message.route.via.authority.getHost()) {
            builder.setHost(message.route.via.authority.getHost());
            if (0 < message.route.via.authority.port) {
                builder.setPort(message.route.via.authority.port);
            }
        }
        builder.setPath(fixedPath.toString());

        for (Map.Entry<WireValue, WireValue> e : message.parameters.entrySet()) {
            WireValue key = e.getKey();
            WireValue value = e.getValue();
            if (!P_CONTENT.equals(key) && !pathVariableNames.contains(key.toText())) {
                builder.addParameter(key.toText(), value.toText());
            }
        }

        return builder.build();

    }


    /////// HTTPCLIENT CONVERSIONS ///////

    private static final String H_PRAGMA_ID_PREFIX = "nextop-id";
    // FIXME serialID
    private static final String H_PRAGMA_PREFIX = "nextop-header";
    // FIXME use something standard - figure out what
    private static final String H_PRAGMA_IMAGE_SIZE_PREFIX = "image-size";


    // IMAGE
    // FIXME maxTransferWidth, maxTransferHeight


    public static HttpHost toHttpHost(Message message) throws URISyntaxException {
        switch (message.route.via.scheme) {
            case HTTP:
            case HTTPS:
                break;
            default:
                throw new URISyntaxException(message.route.toString(), "Bad scheme");
        }

        String host = message.route.via.authority.getHost();
        if (null == host) {
            throw new URISyntaxException(message.route.toString(), "Bad host");
        }

        int port = message.route.via.authority.port;
        if (port <= 0) {
            // apply default
            switch (message.route.via.scheme) {
                case HTTP:
                    port = 80;
                    break;
                case HTTPS:
                    port = 443;
                    break;
                default:
                    // should never reach here; see check above
                    throw new IllegalStateException();
            }
        }

        return new HttpHost(host, port,
                message.route.via.scheme.toString().toLowerCase());
    }


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
            case PUT: {
                HttpPut put = new HttpPut();
                put.setURI(message.toUri());
                attachHeaders(message, put);
                attachContent(message, put);
                return put;
            }
            default:
                throw new IllegalArgumentException();
        }
    }

    // requires a route set into the builder to build
    public static Builder fromHttpResponse(HttpResponse response) {

        Message.Builder builder = Message.newBuilder();

        builder.setCode(response.getStatusLine().getStatusCode());
        builder.setReason(response.getStatusLine().getReasonPhrase());

        attachHeaders(response, builder);
        attachContent(response, builder);

        return builder;
    }

    /** inverse of {@link #toHttpRequest} */
    public static Message fromHttpRequest(HttpUriRequest request) {
        // FIXME 0.1.1
        return null;
    }

    /** inverse of {@link #fromHttpResponse} */
    public static HttpResponse toHttpResponse(Message message) {
        // FIXME 0.1.1
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
            HttpEntity entity;
            MediaType contentType = getContentType(message, content);

            // attach the content type header if missing
            if (!request.containsHeader(HttpHeaders.CONTENT_TYPE)) {
                request.setHeader(HttpHeaders.CONTENT_TYPE, contentType.toString());
            }

            if (contentType.is(MediaType.JSON_UTF_8)) {
                try {
                    entity = new StringEntity(content.toJson());
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(e);
                }
            } else if (contentType.is(MediaType.ANY_TEXT_TYPE)) {
                try {
                    entity = new StringEntity(content.toText());
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(e);
                }
            } else if (contentType.is(MediaType.ANY_IMAGE_TYPE)) {
                switch (content.getType()) {
                    case IMAGE:
                        EncodedImage image = content.asImage();
                        // FIXME 0.1.1
//                        entity = new ByteArrayEntity(image.bytes, image.offset, image.length);
                        entity = new ByteArrayEntity(Arrays.copyOfRange(image.bytes, image.offset, image.length));

                        if (0 < image.width || 0 < image.height) {
                            // attach headers for image width and height
                            request.addHeader(HttpHeaders.PRAGMA, String.format("%s %d %d",
                                    H_PRAGMA_IMAGE_SIZE_PREFIX, image.width, image.height));
                        }
                        break;
                    default:
                        ByteBuffer bb = content.asBlob();
                        byte[] bytes = new byte[bb.remaining()];
                        bb.get(bytes);
                        entity = new ByteArrayEntity(bytes);
                        break;
                }
            } else {
                // send as binary
                ByteBuffer bb = content.asBlob();
                byte[] bytes = new byte[bb.remaining()];
                bb.get(bytes);
                entity = new ByteArrayEntity(bytes);
            }

            // do not set the Content-Length header - the http exec chain does that
//            request.setHeader(HttpHeaders.CONTENT_LENGTH, "" + entity.getContentLength());

            request.setEntity(entity);


            // TODO
//            if ("http".equalsIgnoreCase(request.getURI().getScheme())
//                    && !message.headers.containsKey(WireValue.of(HttpHeaders.CONTENT_MD5))) {
//                request.setHeader(HttpHeaders.CONTENT_MD5, WireValue.of(contentMd5(entity)).toString());
//            } // else https already checks integrity of message
        }
    }

    private static MediaType getContentType(Message message, WireValue content) {
        @Nullable WireValue contentTypeValue = message.headers.get(WireValue.of(HttpHeaders.CONTENT_TYPE));
        if (null != contentTypeValue) {
            return MediaType.parse(contentTypeValue.asString());
        }
        // else use a default content type
        switch (content.getType()) {
            case IMAGE:
                switch (content.asImage().format) {
                    case WEBP:
                        return MediaType.WEBP;
                    case JPEG:
                        return MediaType.JPEG;
                    case PNG:
                        return MediaType.PNG;
                    default:
                        throw new IllegalArgumentException();
                }
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

        @Nullable Header contentTypeHeader = entity.getContentType();
        if (null == contentTypeHeader) {
            contentTypeHeader = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        }


        WireValue value;
        MediaType contentType;
        if (null != contentTypeHeader) {
            contentType = MediaType.parse(contentTypeHeader.getValue());
        } else {
            contentType = MediaType.APPLICATION_BINARY;
        }

        if (contentType.is(MediaType.JSON_UTF_8)) {
            RepetableEntity re = RepetableEntity.create(entity);
            try {
                value = WireValue.valueOfJson(new InputStreamReader(re.entity.getContent(), Charsets.UTF_8));
            } catch (IOException e) {
                // return as text
                try {
                    Reader r = new InputStreamReader(re.entity.getContent(), Charsets.UTF_8);
                    try {
                        return WireValue.of(CharStreams.toString(r));
                    } finally {
                        r.close();
                    }
                } catch (IOException e2) {
                    throw new IllegalArgumentException(e2);
                }
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
        } else if (contentType.is(MediaType.ANY_IMAGE_TYPE)) {
            // receive as image if supported (TODO or can be transcoded)
            // otherwise receive as binary
            RepetableEntity re = RepetableEntity.create(entity);
            re.setBytes();

            EncodedImage.Orientation orientation = EncodedImage.Orientation.REAR_FACING;
            EncodedImage.Format format;
            if (contentType.is(MediaType.JPEG)) {
                format = EncodedImage.Format.JPEG;
            } else if (contentType.is(MediaType.WEBP)) {
                format = EncodedImage.Format.WEBP;
            } else if (contentType.is(MediaType.PNG)) {
                format = EncodedImage.Format.PNG;
            } else {
                // the image is not supported
                // TODO consider doing a transcoding here if possible
                // TODO for now, just surface as binary
                return WireValue.of(re.bytes, re.offset, re.length);
            }

            // FIXME accept image size headers (still need to figure out what's standard)
            int width = EncodedImage.UNKNOWN_WIDTH;
            int height = EncodedImage.UNKNOWN_HEIGHT;
            return WireValue.of(EncodedImage.create(format, orientation, width, height, re.bytes, re.offset, re.length));
        } else {
            // receive as binary
            RepetableEntity re = RepetableEntity.create(entity);
            re.setBytes();

            return WireValue.of(re.bytes, re.offset, re.length);
        }
        return value;
    }


    static final class RepetableEntity {
        static RepetableEntity create(HttpEntity entity) {

            // create a repeatable entity
            // this is needed because the type will fall back if parsing fails
            HttpEntity repeatableEntity;
            @Nullable byte[] bytes = null;
            int offset = 0;
            int length = 0;
            if (entity.isRepeatable()) {
                repeatableEntity = entity;
            } else {
                int contentLength = (int) entity.getContentLength();
                if (contentLength < 0) {
                    contentLength = 1024;
                }
                NoCopyByteArrayOutputStream out = new NoCopyByteArrayOutputStream(contentLength);
                try {
                    try {
                        InputStream is = entity.getContent();
                        try {
                            ByteStreams.copy(is, out);
                        } finally {
                            is.close();
                        }
                    } finally {
                        out.close();
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
                bytes = out.getBytes();
                offset = out.getOffset();
                length = out.getLength();
                repeatableEntity = new ByteArrayEntity(bytes, offset, length);
            }

            if (!repeatableEntity.isRepeatable()) {
                throw new IllegalStateException();
            }

            return new RepetableEntity(repeatableEntity, bytes, offset, length);
        }


        final HttpEntity entity;
        @Nullable byte[] bytes;
        int offset;
        int length;


        RepetableEntity(HttpEntity entity, @Nullable byte[] bytes, int offset, int length) {
            this.entity = entity;
            this.bytes = bytes;
            this.offset = offset;
            this.length = length;
        }


        void setBytes() {
            if (null == bytes) {
                // TODO this could be improved by extracting the buffer from the repeatable entity

                try {
                    InputStream is = entity.getContent();
                    try {
                        bytes = ByteStreams.toByteArray(is);
                        offset = 0;
                        length = bytes.length;
                    } finally {
                        is.close();
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

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



    // FIXME in general, be able to set the response group and priority for any message


    /////// LAYERS ///////

    public static final class LayerInfo {
        public static enum Quality {
            HIGH,
            LOW
        }
        // the default quality of an image is HIGH
        // TODO
        // if LOW and no scale, this will force a recode to a smaller, lower quality image


        public final Quality quality;
        public final EncodedImage.Format format;

        // size: <= 0 means "original" or "aspect-aware scaled" if the other dim is pinned
        public final int width;
        public final int height;
        // TODO can attach other transform params here: scale type, crop, etc

        public final @Nullable Id groupId;
        public final int groupPriority;


        public LayerInfo(Quality quality, EncodedImage.Format format, int width, int height,
                  @Nullable Id groupId, int groupPriority) {
            this.quality = quality;
            this.format = format;
            this.width = width;
            this.height = height;
            this.groupId = groupId;
            this.groupPriority = groupPriority;
        }


        // scale or quality change
        public boolean isTransform() {
            return 0 < width || 0 < height
                    || !Quality.HIGH.equals(quality);
        }
        public int scaleWidth(int w, int h) {
            if (0 < width) {
                // pinned
                return width;
            }
            if (0 < height) {
                // aspect scale
                return w * height / h ;
            }
            // track
            return w;
        }
        public int scaleHeight(int w, int h) {
            if (0 < height) {
                // pinned
                return height;
            }
            if (0 < width) {
                // aspect scale
                return h * width / w;
            }
            // track
            return h;
        }


        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append(quality.toString().toLowerCase())
                .append(" ")
                .append(format.toString().toLowerCase())
                .append(" ")
                .append(0 < width ? width : 0)
                .append(":")
                .append(0 < height ? height : 0);
            if (null != groupId) {
                sb.append(" ")
                    .append(groupId)
                    .append(":")
                    .append(groupPriority);
            }
            return sb.toString();
        }

        public static LayerInfo valueOf(String s) {
            String[] parts = s.split(" ");
            if (parts.length < 3) {
                throw new IllegalArgumentException();
            }

            Quality quality = Quality.valueOf(parts[0].toUpperCase());
            EncodedImage.Format format = EncodedImage.Format.valueOf(parts[1].toUpperCase());

            String[] sizeParts = parts[2].split(":");
            if (2 != sizeParts.length) {
                throw new IllegalArgumentException();
            }
            int width = Integer.parseInt(sizeParts[0]);
            int height = Integer.parseInt(sizeParts[1]);

            @Nullable Id groupId;
            int groupPriority;
            if (4 <= parts.length) {
                // group
                String[] groupParts = parts[3].split(":");
                if (2 != groupParts.length) {
                    throw new IllegalArgumentException();
                }
                groupId = Id.valueOf(groupParts[0]);
                groupPriority = Integer.parseInt(groupParts[1]);
            } else {
                groupId = null;
                groupPriority = 0;
            }

            return new LayerInfo(quality, format, width, height,
                    groupId, groupPriority);
        }

    }


    //

    public static final WireValue H_LAYERS = WireValue.of("$layers");


    @Nullable
    public static LayerInfo[] getLayers(Message message) {
        @Nullable WireValue layersValue = message.headers.get(H_LAYERS);
        if (null == layersValue) {
            return null;
        }

        switch (layersValue.getType()) {
            case UTF8:
                String s = layersValue.asString();
                String[] parts = s.split(";");
                int n = parts.length;
                LayerInfo[] layers = new LayerInfo[n];
                for (int i = 0; i < n; ++i) {
                    layers[i] = LayerInfo.valueOf(parts[i]);
                }
                return layers;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static void setLayers(Message.Builder builder, LayerInfo ... layers) {
        int n = layers.length;
        if (n <= 0) {
            builder.setHeader(H_LAYERS, null);
        } else {
            StringBuilder sb = new StringBuilder(128);
            sb.append(layers[0].toString());
            for (int i = 1; i < n; ++i) {
                sb.append(";").append(layers[i].toString());
            }
            builder.setHeader(H_LAYERS, WireValue.of(sb.toString()));
        }
    }


}
