package io.nextop;

import com.google.common.base.Charsets;
import com.google.common.base.Utf8;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.*;
import java.util.zip.GZIPOutputStream;

// more efficient codec than text, that allows a lossless (*or nearly, for floats) conversion to text when needed
// like protobufs/bson but focused on easy conversion to text
// conversion of text is important when constructing HTTP requests, which are test-based
// TODO do compression of values and objects keysets by having a header in the front. up to the 127 that save the most bytes to compress
// TODO long term the compression table can be stateful, so transferring the same objects repeatedly will have a savings, like a dynamic protobuf
// TODO wire value even in a simple form will increase speed between cloud and client, because binary will be less data and faster to parse on client
// TODO can always call toString to get JSON out (if type is array or object)

// FIXME look at sizes if the lut is stateful, and the lut didn't need to be resent each time
public abstract class WireValue {
    // just use int constants here
    static enum Type {
        UTF8,
        BLOB,
        INT32,
        INT64,
        FLOAT32,
        FLOAT64,
        BOOLEAN,
        MAP,
        LIST

    }





    // FIXME parse
//    public static WireValue valueOf(byte[] bytes, int offset, int n) {
//
//        // expect compressed format here
//
//    }
//

    public static WireValue valueOf(JsonElement e) {
        if (e.isJsonPrimitive()) {
            JsonPrimitive p = e.getAsJsonPrimitive();
            if (p.isBoolean()) {
                return of(p.getAsBoolean());
            } else if (p.isNumber()) {
                return of(p.getAsNumber());
            } else if (p.isString()) {
                return of(p.getAsString());
            } else {
                throw new IllegalArgumentException();
            }
        } else if (e.isJsonObject()) {
            JsonObject object = e.getAsJsonObject();
            Map<WireValue, WireValue> m = new HashMap<WireValue, WireValue>(4);
            for (Map.Entry<String, JsonElement> oe : object.entrySet()) {
                m.put(of(oe.getKey()), valueOf(oe.getValue()));
            }
            return of(m);
        } else if (e.isJsonArray()) {
            JsonArray array = e.getAsJsonArray();
            List<WireValue> list = new ArrayList<WireValue>(4);
            for (JsonElement ae : array) {
                list.add(valueOf(ae));
            }
            return of(list);
        } else {
            throw new IllegalArgumentException();
        }
    }


    public static WireValue of(Object value) {
        // FIXME if wire value, return
        // FIXME else call into the right of(...)
        if (value instanceof Integer) {
            return of(((Integer) value).intValue());
        }
        if (value instanceof Long) {
            return of(((Long) value).longValue());
        }
        if (value instanceof Float) {
            return of(((Float) value).floatValue());
        }
        if (value instanceof Double) {
            return of(((Double) value).doubleValue());
        }
        throw new IllegalArgumentException();
    }

    static WireValue of(String value) {
        return new Utf8WireValue(value);
    }

    static WireValue of(int value) {
        return new NumberWireValue(value);
    }

    static WireValue of(long value) {
        return new NumberWireValue(value);
    }

    static WireValue of(float value) {
        return new NumberWireValue(value);
    }

    static WireValue of(double value) {
        return new NumberWireValue(value);
    }

    static WireValue of(boolean value) {
        return new BooleanWireValue(value);
    }

    static WireValue of(Map<WireValue, WireValue> m) {
        return new MapWireValue(m);
    }

    static WireValue of(List<WireValue> list) {
        return new ListWireValue(list);
    }








    final Type type;

    WireValue(Type type) {
        this.type = type;
    }


    // FIXME as* functions that represent the wirevalue as something



    public Type getType() {
        return type;
    }




    public String toString() {
        // blob to base64
        // object, array to json (string values underneath get quoted)
        // others to standard string forms
        // FIXME

        StringBuilder sb = new StringBuilder();

        toString(this, sb, false, 0);
        return sb.toString();
    }


    void toString(WireValue value, StringBuilder sb, boolean q, int qe) {
        switch (value.getType()) {
            case UTF8:
                if (q) {
                    sb.append(q(qe)).append(value.asString()).append(q(qe));
                } else {
                    sb.append(value.asString());
                }
                break;
            case BLOB:
                if (q) {
                    sb.append(q(qe)).append(base64(value.asBlob())).append(q(qe));
                } else {
                    sb.append(base64(value.asBlob()));
                }
                break;
            case INT32:
                sb.append(value.asInt());
                break;
            case INT64:
                sb.append(value.asLong());
                break;
            case FLOAT32:
                sb.append(value.asFloat());
                break;
            case FLOAT64:
                sb.append(value.asDouble());
                break;
            case BOOLEAN:
                sb.append(value.asBoolean());
                break;
            case MAP: {
                sb.append("{");
                int c = 0;
                for (Map.Entry<WireValue, WireValue> e : value.asMap().entrySet()) {
                    if (1 < ++c) {
                        sb.append(",");
                    }
                    sb.append(q(qe));
                    toString(e.getKey(), sb, false, qe + 1);
                    sb.append(q(qe)).append(":");
                    toString(e.getValue(), sb, true, qe);
                }
                sb.append("}");
                break;
            }
            case LIST:
                sb.append("[");
                int c = 0;
                for (WireValue v : value.asList()) {
                    if (1 < ++c) {
                        sb.append(",");
                    }
                    toString(v, sb, true, qe);
                }
                sb.append("]");
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
    static String q(int e) {
        StringBuilder sb = new StringBuilder(e + 1);
        for (int i = 0; i < e; ++i) {
            sb.append("\\");
        }
        sb.append("\"");
        return sb.toString();
    }

    static String base64(byte[] bytes) {
        // FIXME
        return "";
    }




    static final byte H_COMPRESSED = (byte) 0x80;
    static final byte H_UTF8 = 1;
    static final byte H_BLOB = 2;
    static final byte H_INT32 = 3;
    static final byte H_INT64 = 4;
    static final byte H_FLOAT32 = 5;
    static final byte H_FLOAT64 = 6;
    static final byte H_TRUE_BOOLEAN = 7;
    static final byte H_FALSE_BOOLEAN = 8;
    static final byte H_MAP = 9;
    static final byte H_LIST = 10;
    static final byte H_INT32_LIST = 11;
    static final byte H_INT64_LIST = 12;
    static final byte H_FLOAT32_LIST = 13;
    static final byte H_FLOAT64_LIST = 14;


    static byte[] header(int nb, int v) {
        switch (nb) {
            case 1:
                return new byte[]{(byte) (v & 0xFF)};
            case 2:
                return new byte[]{(byte) ((v >>> 8) & 0xFF), (byte) (v & 0xFF)};
            case 3:
                return new byte[]{(byte) ((v >>> 16) & 0xFF), (byte) ((v >>> 8) & 0xFF), (byte) (v & 0xFF)};
            default:
                throw new IllegalArgumentException();
        }
    }

    static byte listh(List<WireValue> list) {
        int n = list.size();
        if (0 == n) {
            return H_LIST;
        }
        Type t = list.get(0).getType();
        for (int i = 0; i < n; ++i) {
            if (!t.equals(list.get(1).getType())) {
                return H_LIST;
            }
        }
        switch (t) {
            case INT32:
                return H_INT32;
            case INT64:
                return H_INT64_LIST;
            case FLOAT32:
                return H_FLOAT32_LIST;
            case FLOAT64:
                return H_FLOAT64_LIST;
            default:
                return H_LIST;
        }
    }




    // toByte should always compress


    public void toBytes(ByteBuffer bb) {

        Lb lb = new Lb();
        lb.init(this);
        if (lb.opt()) {
            // write the lut header
            byte[] header = header(lb.lutNb, lb.lut.size());
            header[0] |= H_COMPRESSED;
            bb.put(header);
            bb.putInt(0);
            int i = bb.position();

            for (Lb.S s : lb.lut) {
                _toBytes(s.value, lb, bb);
            }

            int bytes = bb.position() - i;
            bb.putInt(i - 4, bytes);
            System.out.printf("header %d bytes\n", bytes);
        }

        int i = bb.position();
        toBytes(this, lb, bb);
        int bytes = bb.position() - i;
        System.out.printf("body %d bytes\n", bytes);

    }
    private void toBytes(WireValue value, Lb lb, ByteBuffer bb) {
        int luti = lb.luti(value);
        if (0 <= luti) {
            byte[] header = header(lb.lutNb, luti);
            header[0] |= H_COMPRESSED;
            bb.put(header);
        } else {
            _toBytes(value, lb, bb);
        }
    }
    private void _toBytes(WireValue value, Lb lb, ByteBuffer bb) {
        switch (value.getType()) {
            case MAP: {
                bb.put(H_MAP);
                bb.putInt(0);

                int i = bb.position();

                Map<WireValue, WireValue> m = value.asMap();
                List<WireValue> keys = stableKeys(m);
                toBytes(of(keys), lb, bb);
                List<WireValue> values = stableValues(m, keys);
                toBytes(of(values), lb, bb);

                int bytes = bb.position() - i;
                bb.putInt(i - 4, bytes);

                break;
            }
            case LIST: {
                List<WireValue> list = value.asList();
                byte listh = listh(list);
                if (H_LIST == listh) {
                    bb.put(H_LIST);

                    bb.putInt(0);

                    int i = bb.position();

                    for (WireValue v : value.asList()) {
                        toBytes(v, lb, bb);
                    }

                    int bytes = bb.position() - i;
                    bb.putInt(i - 4, bytes);
                } else {
                    // primitive homogeneous list
                    bb.put(listh);

                    bb.putInt(0);

                    int i = bb.position();

                    switch (listh) {
                        case H_INT32_LIST:
                            for (WireValue v : value.asList()) {
                                bb.putInt(v.asInt());
                            }
                            break;
                        case H_INT64_LIST:
                            for (WireValue v : value.asList()) {
                                bb.putLong(v.asLong());
                            }
                            break;
                        case H_FLOAT32_LIST:
                            for (WireValue v : value.asList()) {
                                bb.putFloat(v.asFloat());
                            }
                            break;
                        case H_FLOAT64_LIST:
                            for (WireValue v : value.asList()) {
                                bb.putDouble(v.asDouble());
                            }
                            break;
                    }

                    int bytes = bb.position() - i;
                    bb.putInt(i - 4, bytes);

                }
                break;
            }
            case BLOB: {
                byte[] b = value.asBlob();

                bb.put(H_BLOB);
                bb.putInt(b.length);
                bb.put(b);

                break;
            }
            case UTF8: {
                byte[] b = value.asString().getBytes(Charsets.UTF_8);

                bb.put(H_UTF8);
                bb.putInt(b.length);
                bb.put(b);

                break;
            }
            case INT32:
                bb.put(H_INT32);
                bb.putInt(value.asInt());
                break;
            case INT64:
                bb.put(H_INT64);
                bb.putLong(value.asLong());
                break;
            case FLOAT32:
                bb.put(H_FLOAT32);
                bb.putFloat(value.asFloat());
                break;
            case FLOAT64:
                bb.put(H_FLOAT64);
                bb.putDouble(value.asDouble());
                break;
            case BOOLEAN:
                bb.put(value.asBoolean() ? H_TRUE_BOOLEAN : H_FALSE_BOOLEAN);
                break;
        }
    }


    static class Lb {
        static class S {
            WireValue value;

            int count;

            // FIXME record this in expandOne/expand
            int maxd = -1;
            int maxi = -1;
            boolean r = false;

            int luti = -1;
        }


        Map<WireValue, S> stats = new HashMap<WireValue, S>(4);
        List<S> rs = new ArrayList<S>(4);

        List<S> lut = new ArrayList<S>(4);
        int lutNb = 0;


        int luti(WireValue value) {
            S s = stats.get(value);
            assert null != s;
            return s.luti;
        }


        void init(WireValue value) {
            expand(value, 0, 0);
        }

        int expand(WireValue value, int d, int i) {
            switch (value.getType()) {
                case MAP:
                    expandOne(value, d, i, true);
                    i += 1;
                    // the rest
                    Map<WireValue, WireValue> m = value.asMap();
                    List<WireValue> keys = stableKeys(m);
                    i = expand(of(keys), d + 1, i);
                    List<WireValue> values = stableValues(m, keys);
                    i = expand(of(values), d + 1, i);
                    break;
                case LIST:
                    expandOne(value, d, i, true);
                    i += 1;
                    // the rest
                    for (WireValue v : value.asList()) {
                        i = expand(v, d + 1, i);
                    }
                    break;
                default:
                    expandOne(value, d, i, false);
                    i += 1;
                    break;
            }
            return i;
        }
        void expandOne(WireValue value, int d, int i, boolean rmask) {
            S s = stats.get(value);
            if (null == s) {
                s = new S();
                s.value = value;
                stats.put(value, s);
            }
            s.count += 1;
            s.maxd = Math.max(s.maxd, d);
            s.maxi = Math.max(s.maxi, i);
            if (rmask && !s.r) {
                s.r = true;
                rs.add(s);
            }
        }

        boolean opt() {

            int nb = estimateLutNb();
            if (nb <= 0) {
                return false;
            }

            // order r values by maxd
            // if include each, subtract down
            Collections.sort(rs, new Comparator<S>() {
                @Override
                public int compare(S a, S b) {
                    if (a == b) {
                        return 0;
                    }
                    int d = Integer.compare(a.maxd, b.maxd);
                    if (0 != d) {
                        return d;
                    }
                    d = Integer.compare(a.maxi, b.maxi);
                    assert 0 != d;
                    return d;
                }
            });
            for (S s : rs) {
                if (include(s, nb)) {
                    s.luti = lut.size();
                    lut.add(s);
                    collapse(s.value);
                }
            }

            lutNb = nb(lut.size());

            // lut is ordered implicity by maxd

            return true;
        }

        void collapse(WireValue value) {
            switch (value.getType()) {
                case MAP:
                    collapseOne(value);
                    // the rest
                    Map<WireValue, WireValue> m = value.asMap();
                    List<WireValue> keys = stableKeys(m);
                    collapse(of(keys));
                    List<WireValue> values = stableValues(m, keys);
                    collapse(of(values));
                    break;
                case LIST:
                    collapseOne(value);
                    // the rest
                    for (WireValue v : value.asList()) {
                        collapse(v);
                    }
                    break;
                default:
                    collapseOne(value);
                    break;
            }
        }
        void collapseOne(WireValue value) {
            S s = stats.get(value);
            assert null != s;
            s.count -= 1;
        }





        int[] sizes = new int[]{0, 0x10, 0x0100, 0x010000, 0x01000000};
        int nb(int c) {
            int nb = 0;
            while (sizes[nb] < c) {
                ++nb;
            }
            return nb;
        }






        int estimateLutNb() {
            int nb = nb(count(1));
            if (nb == sizes.length) {
                // too many unique values; punt
                return -1;
            }
            int pnb;
            do {
                pnb = nb;
                nb = nb(count(nb));
            } while (nb < pnb);

            return pnb;
        }
        int count(int nb) {
            int c = 0;
            for (S s : stats.values()) {
                if (include(s, nb)) {
                    c += 1;
                }
            }
            return c;
        }


        boolean include(S s, int nb) {
            int b = quickLowerSize(s.value.getType());
            return nb * s.count + b < s.count * b;
        }

        int quickLowerSize(Type type) {
            switch (type) {
                case MAP:
                    // TODO this is a lower estimate
                    // length + ...
                    return 12;
                case LIST:
                    // TODO this is a lower estimate
                    return 8;
                case BLOB:
                    // TODO this is a lower estimate
                    return 8;
                case UTF8:
                    // TODO this is a lower estimate
                    return 8;
                case INT32:
                    return 4;
                case INT64:
                    return 8;
                case FLOAT32:
                    return 4;
                case FLOAT64:
                    return 8;
                case BOOLEAN:
                    return 1;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    static List<WireValue> stableKeys(Map<WireValue, WireValue> m) {
        List<WireValue> keys = new ArrayList<WireValue>(m.keySet());
        Collections.sort(keys, stableComparator);
        return keys;
    }

    static List<WireValue> stableValues(Map<WireValue, WireValue> m, List<WireValue> keys) {
        List<WireValue> values = new ArrayList<WireValue>(keys.size());
        for (WireValue key : keys) {
            values.add(m.get(key));
        }
        return values;
    }


    static final Comparator<WireValue> stableComparator = new StableComparator();

    // stable ordering for all values
    static final class StableComparator implements Comparator<WireValue> {
        @Override
        public int compare(WireValue a, WireValue b) {
            Type ta = a.getType();
            Type tb = b.getType();

            int d = ta.ordinal() - tb.ordinal();
            if (0 != d) {
                return d;
            }
            assert ta.equals(tb);

            switch (ta) {
                case MAP: {
                    Map<WireValue, WireValue> amap = a.asMap();
                    Map<WireValue, WireValue> bmap = b.asMap();
                    d = amap.size() - bmap.size();
                    if (0 != d) {
                        return d;
                    }
                    List<WireValue> akeys = stableKeys(amap);
                    List<WireValue> bkeys = stableKeys(bmap);
                    d = compare(of(akeys), of(bkeys));
                    if (0 != d) {
                        return d;
                    }
                    return compare(of(stableValues(amap, akeys)), of(stableValues(bmap, bkeys)));
                }
                case LIST: {
                    List<WireValue> alist = a.asList();
                    List<WireValue> blist = b.asList();
                    int n = alist.size();
                    int m = blist.size();
                    d = n - m;
                    if (0 != d) {
                        return d;
                    }
                    for (int i = 0; i < n; ++i) {
                        d = compare(alist.get(i), blist.get(i));
                        if (0 != d) {
                            return d;
                        }
                    }
                    return 0;
                }
                case BLOB: {
                    byte[] abytes = a.asBlob();
                    byte[] bbytes = b.asBlob();
                    int n = abytes.length;
                    int m = bbytes.length;
                    d = n - m;
                    if (0 != d) {
                        return d;
                    }
                    for (int i = 0; i < n; ++i) {
                        d = (0xFF & abytes[i]) - (0xFF & bbytes[i]);
                        if (0 != d) {
                            return d;
                        }
                    }
                    return 0;
                }
                case UTF8:
                    return a.asString().compareTo(b.asString());
                case INT32:
                    return Integer.compare(a.asInt(), b.asInt());
                case INT64:
                    return Long.compare(a.asLong(), b.asLong());
                case FLOAT32:
                    return Float.compare(a.asFloat(), b.asFloat());
                case FLOAT64:
                    return Double.compare(a.asDouble(), b.asDouble());
                default:
                    throw new IllegalArgumentException();
            }
        }
    }





    // logical view, regardless of wire format

    abstract String asString();
    abstract int asInt();
    abstract long asLong();
    abstract float asFloat();
    abstract double asDouble();
    abstract boolean asBoolean();
    abstract List<WireValue> asList();
    abstract Map<WireValue, WireValue> asMap();
    abstract byte[] asBlob();









    private static class Utf8WireValue extends WireValue {
        final String value;

        Utf8WireValue(String value) {
            super(Type.UTF8);
            this.value = value;
        }

        public String asString() {
            return value;
        }

        public int asInt() {
            return Integer.parseInt(value);
        }

        public long asLong() {
            return Long.parseLong(value);
        }
        public float asFloat() {
            return Float.parseFloat(value);
        }
        public double asDouble() {
            return Double.parseDouble(value);
        }
        public boolean asBoolean() {
            return Boolean.parseBoolean(value);
        }
        public List<WireValue> asList() {
            throw new UnsupportedOperationException();
        }
        public Map<WireValue, WireValue> asMap() {
            throw new UnsupportedOperationException();
        }
        public byte[] asBlob() {
            return value.getBytes(Charsets.UTF_8);
        }


        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Utf8WireValue)) {
                return false;
            }
            Utf8WireValue b = (Utf8WireValue) obj;
            return value.equals(b.value);
        }
    }

    private static class NumberWireValue extends WireValue {
        static Type type(Number n) {
            if (n instanceof Integer) {
                return Type.INT32;
            }
            if (n instanceof Long) {
                return Type.INT64;
            }
            if (n instanceof Float) {
                return Type.FLOAT32;
            }
            if (n instanceof Double) {
                return Type.FLOAT64;
            }
            throw new IllegalArgumentException();
        }

        final Number value;

        NumberWireValue(Number value) {
            super(type(value));
            this.value = value;
        }

        public String asString() {
            return value.toString();
        }

        public int asInt() {
            return value.intValue();
        }

        public long asLong() {
            return value.longValue();
        }
        public float asFloat() {
            return value.floatValue();
        }
        public double asDouble() {
            return value.doubleValue();
        }
        public boolean asBoolean() {
            // c style
            return 0 != value.floatValue();
        }
        public List<WireValue> asList() {
            throw new UnsupportedOperationException();
        }
        public Map<WireValue, WireValue> asMap() {
            throw new UnsupportedOperationException();
        }
        public byte[] asBlob() {
            // TODO
            throw new UnsupportedOperationException();
        }


        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof NumberWireValue)) {
                return false;
            }
            NumberWireValue b = (NumberWireValue) obj;
            return value.equals(b.value);
        }
    }

    private static class BooleanWireValue extends WireValue {

        final boolean value;

        BooleanWireValue(boolean value) {
            super(Type.BOOLEAN);
            this.value = value;
        }

        public String asString() {
            return String.valueOf(value);
        }

        public int asInt() {
            return value ? 1 : 0;
        }

        public long asLong() {
            return value ? 1L : 0L;
        }
        public float asFloat() {
            return value ? 1.f : 0.f;
        }
        public double asDouble() {
            return value ? 1.0 : 0.0;
        }
        public boolean asBoolean() {
            return value;
        }
        public List<WireValue> asList() {
            throw new UnsupportedOperationException();
        }
        public Map<WireValue, WireValue> asMap() {
            throw new UnsupportedOperationException();
        }
        public byte[] asBlob() {
            // TODO
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return Boolean.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BooleanWireValue)) {
                return false;
            }
            BooleanWireValue b = (BooleanWireValue) obj;
            return value == b.value;
        }
    }

    private static class ListWireValue extends WireValue {
        final List<WireValue> value;

        ListWireValue(List<WireValue> value) {
            super(Type.LIST);
            this.value = value;
        }

        public String asString() {
            return value.toString();
        }

        public int asInt() {
            throw new UnsupportedOperationException();
        }

        public long asLong() {
            throw new UnsupportedOperationException();
        }
        public float asFloat() {
            throw new UnsupportedOperationException();
        }
        public double asDouble() {
            throw new UnsupportedOperationException();
        }
        public boolean asBoolean() {
            throw new UnsupportedOperationException();
        }
        public List<WireValue> asList() {
            return value;
        }
        public Map<WireValue, WireValue> asMap() {
            throw new UnsupportedOperationException();
        }
        public byte[] asBlob() {
            // TODO
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ListWireValue)) {
                return false;
            }
            ListWireValue b = (ListWireValue) obj;
            return value.equals(b.value);
        }

    }

    private static class MapWireValue extends WireValue {
        final Map<WireValue, WireValue> value;

        MapWireValue(Map<WireValue, WireValue> value) {
            super(Type.MAP);
            this.value = value;
        }

        public String asString() {
            return value.toString();
        }

        public int asInt() {
            throw new UnsupportedOperationException();
        }

        public long asLong() {
            throw new UnsupportedOperationException();
        }
        public float asFloat() {
            throw new UnsupportedOperationException();
        }
        public double asDouble() {
            throw new UnsupportedOperationException();
        }
        public boolean asBoolean() {
            throw new UnsupportedOperationException();
        }
        public List<WireValue> asList() {
            throw new UnsupportedOperationException();
        }
        public Map<WireValue, WireValue> asMap() {
            return value;
        }
        public byte[] asBlob() {
            // TODO
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MapWireValue)) {
                return false;
            }
            MapWireValue b = (MapWireValue) obj;
            return value.equals(b.value);
        }
    }

    // FIXME  ...


    // based on byte[] and views into the byte[] (parsing does not expand into a bunch of objects in memory)
    private static class CompressedWireValue {

    }

    // LUT sizes: 2^7, 2^15
    // index maps to byte[]
    private static class CompressionState {

    }
}
