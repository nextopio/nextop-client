package io.nextop;

import com.google.common.base.Charsets;
import com.sun.org.glassfish.external.statistics.Stats;
import org.apache.commons.codec.binary.Base64;

import java.nio.ByteBuffer;
import java.util.*;

// more efficient codec than text, that allows a lossless (*or nearly, for floats) conversion to text when needed
// like protobufs/bson but focused on easy conversion to text
// conversion of text is important when constructing HTTP requests, which are test-based
// TODO do compression of values and objects keysets by having a header in the front. up to the 127 that save the most bytes to compress
// TODO long term the compression table can be stateful, so transferring the same objects repeatedly will have a savings, like a dynamic protobuf
// TODO wire value even in a simple form will increase speed between cloud and client, because binary will be less data and faster to parse on client
// TODO can always call toString to get JSON out (if type is array or object)
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
        LIST,
        MAP,

    }


    public static WireValue valueOf(byte[] bytes, int offset, int n) {

        // expect compressed format here

    }


    WireValue of(String value) {

    }

    WireValue of(int value) {

    }

    WireValue of(long value) {

    }

    WireValue of(float value) {

    }

    WireValue of(double value) {

    }

    WireValue of(boolean value) {

    }

    WireValue of(Map<WireValue, WireValue> map) {

    }

    WireValue of(List<WireValue> list) {

    }








    Type type;


    // FIXME as* functions that represent the wirevalue as something



    public Type getType() {

    }




    public String toString() {
        // blob to base64
        // object, array to json (string values underneath get quoted)
        // others to standard string forms
    }

    // toByte should always compress

    public void toBytes(ByteBuffer bb) {

        Lb lb = new Lb();
        lb.all(this);

        lb.opt();


        if (0 < lb.lutNb) {
            // write a lut header
        }

        toBytes(this, lb, bb);
    }
    private void toBytes(WireValue value, Lb lb, ByteBuffer bb) {
        switch (value.getType()) {
            case MAP:
                Map<WireValue, WireValue> m = value.asMap();
                List<WireValue> keys = stableKeys(m.keySet());



        }
    }


    static class Lb {
        static class S {
            int count;

            // FIXME record this in one/all
            int maxDepth = 0;

            int luti;
        }

        Map<WireValue, S> stats;
        int lutNb;
//        WireValue[] lut;

        void all(WireValue value) {
            switch (value.getType()) {
                case MAP:
                    one(value);
                    Map<WireValue, WireValue> m = value.asMap();
                    List<WireValue> keys = stableKeys(m);
                    all(of(keys));
                    List<WireValue> values = stableValues(keys, m);
                    all(of(values));

                    break;
                case LIST:
                    one(value);
                    for (WireValue v : value.asList()) {
                        all(v);
                    }
                    break;
                default:
                    one(value);
                    break;
            }
        }
        void one(WireValue value) {
            S s = stats.get(value);
            if (null == s) {
                s = new S();
                stats.put(value, s);
            }
            s.count += 1;
        }

        void opt() {
            // 2 int no lut:  (1+4)*2
            // 2 int 7b-lut:  (1)*2+4
            // 2 int 15b-lut: (2)*2+4 ** break-even
            // 2 int 23b-lut: (3)*2+4

            // goal: find lowest possible nb
            // c = total for 7 lut
            // nb = sized to fit c
            // do {
            // p = nb
            // c = total for nb lut
            // nb = sized to fit c
            // } while (nb < p)

            // nb = 7
            // nb = 23
            // nb = 7


            // fix the number of bits to use for the lut
            // FIXME this is wrong
            // just compute the lut,
            // then when have the lut,
            // repeatedly trim(...) to remove values that add size at the current lut size
            // this is a local optimization but works with the depth sizing

            int[] sizes = new int[]{0, 0x10, 0x0100, 0x010000, 0x01000000};

            int c = count(1);
            int nb = 0;
            while (sizes[nb] < c) {
                ++nb;
            }
            if (nb == sizes.length) {
                // too many unique values; fix this
                // TODO
                throw new IllegalArgumentException();
            }
            int pnb;
            int pc;
            do {
                pnb = nb;
                pc = c;
                c = count(nb);
                nb = 0;
                while (sizes[nb] < c) {
                    ++nb;
                }
            } while (nb < pnb);

            // use (pc, pnb)
//            lut(pnb);

            lutNb = pnb;
        }
        int count(int nb) {
            int c = 0;
            for (Map.Entry<WireValue, S> e : stats.entrySet()) {
                WireValue value = e.getKey();
                S s = e.getValue();
                if (include(value, s, nb)) {
                    c += 1;
                }
            }
            return c;
        }
        void lut(int nb) {
            // FIXME order by max depth, lowest to highest

            // FIXME when choose one, subtract count from all below it (e.g. map, subtract count from all keys, values)

            lutNb = nb;
            lut = new WireValue[c];
//            re = new HashMap<WireValue, byte[]>(c);
            int i = 0;
            for (Map.Entry<WireValue, S> e : stats.entrySet()) {
                WireValue value = e.getKey();
                S s = e.getValue();
                if (include(value, s, nb)) {
                    lut[i] = value;
                    s.luti = i;

//                    re.put(value, rePrefix(nb, i));
                } else {
                    s.luti = -1;
                }
            }
            assert i == c;
        }
        boolean include(WireValue value, S s, int nb) {
            int b = quickSize(value);
            return nb * s.count + b < s.count * b;
        }

        int quickSize(WireValue value) {
            switch (value.getType()) {
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



    // logical view, regardless of wire format

    abstract String asString();
    abstract int asInt();
    abstract long asLong();
    abstract float asFloat();
    abstract double asDouble();
    abstract boolean asBoolean();
    abstract List<WireValue> asList();
    abstract Map<WireValue, WireValue> asMap();


    private static class StringWireValue {


    }

    private static class NumberWireValue {


    }

    private static class BooleanWireValue {


    }

    private static class ListWireValue {


    }

    private static class MapWireValue {


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
