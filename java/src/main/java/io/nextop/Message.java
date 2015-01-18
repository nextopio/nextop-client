package io.nextop;

import com.google.common.collect.ImmutableMap;
import org.apache.http.client.utils.URIBuilder;
import rx.functions.Func1;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static io.nextop.Nurl.*;

// goals: fast to parse, minimal object creation on parse or each operation
public class Message {
    public static final WireValue P_CONTENT = WireValue.of("$content");



    public static Nurl receiverNurl(Id id) {
        return Nurl.local(Target.create(Method.POST, Path.valueOf(id.toString())));
    }

    public static Message receiverSpec(Id id, int priority) {
        return newBuilder()
                .setNurl(receiverNurl(id))
                .setPriority(priority)
                .build();
    }

    // FIXME echoNurl
    // FIXME statusNurl



    // FIXME
//    public static Message valueOf(URI uri) {
//        // FIXME
//    }
//
//    public static Message valueOf(URL url) {
//        try {
//            return valueOf(url.toURI());
//        } catch (URISyntaxException e) {
//            throw new IllegalArgumentException(e);
//        }
//    }




    public final Id id;
    public final int priority;
    public final Nurl nurl;
    public final Map<WireValue, WireValue> headers;
    public final Map<WireValue, WireValue> parameters;



    private Message(Id id, int priority, Nurl nurl,
                    Map<WireValue, WireValue> headers,
                    Map<WireValue, WireValue> parameters) {
        this.id = id;
        this.priority = priority;
        this.nurl = nurl;
        this.headers = headers;
        this.parameters = parameters;
    }


    public Nurl receiverNurl() {
        return receiverNurl(id);
    }


    public URI toUri() throws URISyntaxException {
        // variables in path get substituted to path;
        // if not all variables can be substituted, throw a URISyntaxException
        // parameters except P_CONTENT get added as query params (via toString)

        Path fixedPath = nurl.target.path.fix(new Func1<String, Object>() {
            @Override
            public Object call(String s) {
                return parameters.get(WireValue.of(s));
            }
        });

        URIBuilder builder = new URIBuilder();
        builder.setScheme(nurl.via.scheme.toString());
        builder.setHost(nurl.via.authority.getHost());
        if (0 < nurl.via.authority.port) {
            builder.setPort(nurl.via.authority.port);
        }
        builder.setPath(fixedPath.toString());

        for (Map.Entry<WireValue, WireValue> e : parameters.entrySet()) {
            WireValue key = e.getKey();
            WireValue value = e.getValue();
            if (!P_CONTENT.equals(key)) {
                builder.addParameter(key.toString(), value.toString());
            }
        }

        return builder.build();
    }



    public Builder toBuilder() {
        Builder b = newBuilder()
                .setPriority(priority)
                .setNurl(nurl);

        for (Map.Entry<WireValue, WireValue> e : headers.entrySet()) {
            b = b.setHeader(e.getKey(), e.getValue());
        }
        for (Map.Entry<WireValue, WireValue> e : parameters.entrySet()) {
            b = b.set(e.getKey(), e.getValue());
        }

        return b;
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private final Id id = Id.create();

        private int priority = 0;
        @Nullable
        private Nurl.Target target = null;
        @Nullable
        private Nurl.Via via = null;

        private Map<WireValue, WireValue> headers = new HashMap<WireValue, WireValue>(8);
        private Map<WireValue, WireValue> parameters = new HashMap<WireValue, WireValue>(8);


        private Builder() {
        }


        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder setTarget(@Nullable Nurl.Target target) {
            this.target = target;
            return this;
        }
        public Builder setTarget(@Nullable String target) {
            if (null != target) {
                this.target = Nurl.Target.valueOf(target);
            } else {
                this.target = null;
            }
            return this;
        }

        public Builder setVia(@Nullable Nurl.Via via) {
            this.via = via;
            return this;
        }
        public Builder setVia(@Nullable String via) {
            if (null != via) {
                this.via = Nurl.Via.valueOf(via);
            } else {
                this.via = null;
            }
            return this;
        }

        public Builder setNurl(@Nullable Nurl nurl) {
            if (null != nurl) {
                this.target = nurl.target;
                this.via = nurl.via;
            } else {
                this.target = null;
                this.via = null;
            }
            return this;
        }
        public Builder setNurl(@Nullable String nurl) {
            if (null != nurl) {
                Nurl n = Nurl.valueOf(nurl);
                target = n.target;
                via = n.via;
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
            Nurl nurl;
            if (null != via) {
                nurl = Nurl.create(target, via);
            } else {
                nurl = Nurl.local(target);
            }
            return new Message(id, priority, nurl,
                    ImmutableMap.copyOf(headers),
                    ImmutableMap.copyOf(parameters));
        }


    }
}
