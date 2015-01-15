package io.nextop;

import javax.annotation.Nullable;
import java.util.Map;


// goals: fast to parse, minimal object creation on parse or each operation
public class Message {
    public static final WireValue P_CONTENT = WireValue.of("$content");


    Id id;

    int priority;
    Nurl nurl;



    public Message toHead() {
        // FIXME remove expand parameters not in Target
        // FIXME keeps expand headers and meta
        return null;
    }

    public Builder toBuilder() {
        // FIXME
        return null;
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {

        // setTarget
        // setVia
        // setPriority
        // addHeader
        // removeHeader
        // addParameter
        // removeParameter
        // setBody
        // removeBody

        private final Id id = Id.create();

        private int priority;
        private Nurl.Target target;
        private Nurl.Via via;

        private Map<WireValue, WireValue> headers;
        private Map<WireValue, WireValue> parameters;


        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder setTarget(@Nullable String target) {
//            this.target = target;
            return this;
        }

        public Builder setVia(@Nullable String via) {
//            this.via = via;
            return this;
        }

        public Builder setNurl(@Nullable String nurl) {
//            if (null != nurl) {
//                target = nurl.target;
//                via = nurl.via;
//            } else {
//                target = null;
//                via = null;
//            }
            return this;
        }




        public Builder putHeader(Object name, @Nullable Object value) {
            if (null != value) {
                headers.put(WireValue.of(name), WireValue.of(value));
            } else {
                headers.remove(WireValue.of(name));
            }
            return this;
        }

        public Builder setBody(@Nullable Object value) {
            return put(P_CONTENT, value);
        }

        public Builder put(Object name, @Nullable Object value) {
            if (null != value) {
                parameters.put(WireValue.of(name), WireValue.of(value));
            } else {
                parameters.remove(WireValue.of(name));
            }
            return this;
        }

        // type-specific header puts

        // type-specific puts




        public Message build() {
//            return new Message(id, priority, new Nurl(target, via),
//                    ImmutableMap.copyOf(headers),
//                    ImmutableMap.copyOf(parameters));
            return null;
        }


    }
}
