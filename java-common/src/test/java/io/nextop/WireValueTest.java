package io.nextop;

import com.google.gson.*;
import io.nextop.util.HexBytes;
import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WireValueTest extends TestCase {


    public void testStableKeys() {

        int n = 8;
        int mm = 32;
        Random r = new Random();

        ByteBuffer bb = ByteBuffer.allocate(32 * 1024);

        for (int i = 0; i < n; ++i) {
            int m = r.nextInt(2 * mm);
            Map<String, Integer> map = new HashMap<String, Integer>(m);
            for (int j = 0; j < m; ++j) {
                String s = randomString(r, 8);
                String key = s;
                for (Integer e = j; null != (e = map.put(key, e)); ) {
                    // double down
                    key += " " + s;
                }
            }

            Map<WireValue, WireValue> valueMap = new HashMap<WireValue, WireValue>(map.size());
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                valueMap.put(WireValue.of(e.getKey()), WireValue.of(e.getValue()));
            }
            assertEquals(map.size(), valueMap.size());

            // to bytes, read back,
            // now compare equals, compare the json

            WireValue in = WireValue.of(valueMap);

            bb.clear();
            in.toBytes(bb);

            bb.flip();
            WireValue out = WireValue.valueOf(bb);

            assertEquals(in, out);
            assertEquals(in.toJson(), out.toJson());
            assertEquals(in.toText(), out.toText());
        }


    }

    public void testToString() {
        // create random values and call #toString, #toText, #toDebugString
        // check that nothing crashes on those calls

        int n = 32;
        Random r = new Random();

        WireValue.Type[] types = WireValue.Type.values();

        for (int i = 0; i < n; ++i) {
            WireValue.Type type = types[r.nextInt(types.length)];

            WireValue value = randomValue(r, type);

            System.out.printf("%s\n", value.toString());
            System.out.printf("%s\n", value.toText());
            System.out.printf("%s\n", value.toDebugString());
        }
    }



    public void testRandomCodec() {

        // 1. generate random json
        // 2. convert to wire value

        // 3. bytes (measure time)
        // 4. parse (3) (measure time)
        // 5. (4) to json string (measure time)
        // 6. parse json (5) (measure time)
        // 7. convert json to wire value
        // 8. assert (4) == (7)


        Random r = new Random();
        JsonElement je = randomJson(r, 6);
        WireValue v = WireValue.of(je);
        ByteBuffer bb = ByteBuffer.allocate(32 * 1024 * 1024);
        for (int i = 0; i < 8; ++i) {
            bb.clear();
            long t = System.nanoTime();
            v.toBytes(bb);
            System.out.printf("to bytes %.3fms\n", ((System.nanoTime() - t) / 1000) / 1000.f);

            bb.flip();
            byte[] bytes = new byte[bb.remaining()];
            bb.get(bytes);
            t = System.nanoTime();
            WireValue v2 = WireValue.valueOf(bytes);
            System.out.printf("parse bytes %.3fms\n", ((System.nanoTime() - t) / 1000) / 1000.f);
            assertEquals(v, v2);

            t = System.nanoTime();
            String js2 = v2.toJson();
            System.out.printf("to json %.3fms\n", ((System.nanoTime() - t) / 1000) / 1000.f);

            t = System.nanoTime();
            JsonElement je2 = new JsonParser().parse(js2);
            System.out.printf("parse json %.3fms\n", ((System.nanoTime() - t) / 1000) / 1000.f);

            t = System.nanoTime();
            WireValue v3 = WireValue.of(je2);
            System.out.printf("value of json %.3fms\n", ((System.nanoTime() - t) / 1000) / 1000.f);
            assertEquals(v, v3);

            v = v3;
        }
    }


    /** from WireValueTest */
    private static JsonElement randomJson(Random r, int d) {
        return randomJson(r, d, 0);
    }
    private static JsonElement randomJson(Random r, int d, int u) {
        // list, map, long, double, string, bool
        int min, max;
        if (0 == u) {
            min = 4;
            max = 13;
        } else if (0 < d) {
            min = 0;
            max = 14;
        } else {
            min = 0;
            max = 4;
        }

        switch (min + r.nextInt(max - min)) {
            case 0:
                return new JsonPrimitive(randomString(r, 16));
            case 1:
                return new JsonPrimitive(r.nextLong());
            case 2:
                return new JsonPrimitive(r.nextDouble());
            case 3:
                return new JsonPrimitive(r.nextBoolean());
            case 4:
            case 5:
            case 6:
            case 7:
            case 8: {
                int n = r.nextInt(16);
                JsonArray array = new JsonArray();
                for (int i = 0; i < n; ++i) {
                    array.add(randomJson(r, d - 1, u + 1));
                }
                return array;
            }
            case 9:
            case 10:
            case 11:
            case 12:
            case 13: {
                int n = r.nextInt(16);
                JsonObject object = new JsonObject();
                for (int i = 0; i < n; ++i) {
                    int m = r.nextInt(32);
                    byte[] keyBytes = new byte[m];
                    r.nextBytes(keyBytes);
                    String key = HexBytes.toString(keyBytes);
                    object.add(key, randomJson(r, d - 1, u + 1));
                }
                return object;
            }
            default:
                throw new IllegalStateException();
        }
    }

    private static String randomString(Random r, int m) {
        int n = r.nextInt(m);
        byte[] bytes = new byte[n];
        r.nextBytes(bytes);
        return HexBytes.toString(bytes);
    }



    private static WireValue randomValue(Random r, WireValue.Type type) {
        switch (type) {
            case UTF8:
                return WireValue.of(randomString(r, 8));
            case BLOB:
                return WireValue.of(randomBytes(r, 32));
            case INT32:
                return WireValue.of(r.nextInt());
            case INT64:
                return WireValue.of(r.nextLong());
            case FLOAT32:
                return WireValue.of(r.nextFloat());
            case FLOAT64:
                return WireValue.of(r.nextDouble());
            case BOOLEAN:
                return WireValue.of(r.nextBoolean());
            case MAP:
                WireValue mapValue;
                do {
                    mapValue = WireValue.of(randomJson(r, 3));
                } while (!WireValue.Type.MAP.equals(mapValue.getType()));
                return mapValue;
            case LIST:
                WireValue listValue;
                do {
                    listValue = WireValue.of(randomJson(r, 3));
                } while (!WireValue.Type.LIST.equals(listValue.getType()));
                return listValue;
            case MESSAGE:
                return WireValue.of(randomMessage(r));
            case IMAGE:
                return WireValue.of(randomImage(r));
            case NULL:
                return WireValue.of();
            default:
                throw new IllegalArgumentException("" + type);
        }
    }

    private static byte[] randomBytes(Random r, int m) {
        int n = r.nextInt(2 * m);
        byte[] bytes = new byte[n];
        r.nextBytes(bytes);
        return bytes;
    }

    private static Message randomMessage(Random r) {
        // TODO random authority, random method, random path
        // TODO random values
        return Message.newBuilder().setRoute("GET http://tests.nextop.io").build();
    }

    private static EncodedImage randomImage(Random r) {
        EncodedImage.Format[] formats = EncodedImage.Format.values();
        EncodedImage.Orientation[] orientations = EncodedImage.Orientation.values();

        EncodedImage.Format format = formats[r.nextInt(formats.length)];
        EncodedImage.Orientation orientation = orientations[r.nextInt(orientations.length)];

        int width = 1 + r.nextInt(512);
        int height = 1 + r.nextInt(512);
        byte[] bytes = randomBytes(r, 1024);

        return EncodedImage.create(format, orientation, width, height, bytes, 0, bytes.length);
    }


}
