package io.nextop.test;

import android.test.InstrumentationTestCase;
import android.util.Log;
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.nextop.*;
import io.nextop.util.HexBytes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class NextopTest extends InstrumentationTestCase {

    Nextop nextop;


    @Override
    protected void setUp() throws Exception {
        super.setUp();

        nextop = Nextop.create(getInstrumentation().getContext()).start();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        nextop = nextop.stop();
    }


    public void testRandomSendReceive() {
        assertNotNull(nextop);
        assertNotNull(nextop.getAuth());


        final long timeoutMs = 6000;


        final int n = 64;
        Random r = new Random();



        final Route.Method[] methods = new Route.Method[]{
                Route.Method.POST
        };
        final MediaType[] responseTypes = new MediaType[]{
                MediaType.JSON_UTF_8,
                // FIXME go through all the types for WireValue send/receive
                // FIXME some are broken
//                MediaType.PLAIN_TEXT_UTF_8,
//                MediaType.APPLICATION_BINARY,
//                MediaType.JPEG,
//                MediaType.PNG,
//                MediaType.WEBP
        };


        Route.Via via = Route.Via.valueOf("http://tests.nextop.io");
        for (int i = 0; i < n; ++i) {
            // methods
            Route.Method method = methods[r.nextInt(methods.length)];

            MediaType responseType = responseTypes[r.nextInt(responseTypes.length)];
            WireValue responseBody = createRandomResponseBody(r, responseType);
            int responseCode = 200;


            Map<WireValue, WireValue> content = new HashMap<WireValue, WireValue>(4);
            {
                Map<WireValue, WireValue> responseHeaders = new HashMap<WireValue, WireValue>(4);
                responseHeaders.put(WireValue.of(HttpHeaders.CONTENT_TYPE), WireValue.of(responseType.toString()));

                Map<WireValue, WireValue> response = new HashMap<WireValue, WireValue>(4);
                response.put(WireValue.of("code"), WireValue.of(responseCode));
                response.put(WireValue.of("headers"), WireValue.of(responseHeaders));
                // send the response body as a string TODO
                response.put(WireValue.of("body"), WireValue.of(responseBody.toString()));


                content.put(WireValue.of("response"), WireValue.of(response));
            }


            Message request = Message.newBuilder()
                            .setRoute(Route.create(Route.Target.create(method, Path.valueOf("/")), via))
                            .setContent(WireValue.of(content))
                            .build();

            Log.i("NextopTest", String.format("Send %s", request));

            Message response;
                response = nextop.send(request)
                .timeout(timeoutMs, TimeUnit.MILLISECONDS)
                .toBlocking().single();

            MediaType actualResponseType = MediaType.parse(response.headers.get(WireValue.of(HttpHeaders.CONTENT_TYPE)).asString());

            assertEquals(responseCode, response.getCode());
            assertTrue(String.format("%s <> %s", responseType, actualResponseType), actualResponseType.is(responseType));
            // isSend that the response parsing worked correctly
            assertEquals(String.format("%s <> %s", responseBody.toDebugString(), response.getContent().toDebugString()),
                    responseBody, response.getContent());
        }
    }

    private WireValue createRandomResponseBody(Random r, MediaType responseType) {
        if (responseType.is(MediaType.JSON_UTF_8)) {
            return WireValue.of(randomJson(r, 2));
        }
        if (responseType.is(MediaType.APPLICATION_BINARY)) {
            return WireValue.of(randomBytes(r, 128));
        }
        if (responseType.is(MediaType.JPEG)) {
            EncodedImage image;
            try {
                image = EncodedImage.jpeg(ByteStreams.toByteArray(getInstrumentation().getContext().getAssets().open("flora.jpg")));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return WireValue.of(image);
        }
        if (responseType.is(MediaType.PNG)) {
            EncodedImage image;
            try {
                image = EncodedImage.jpeg(ByteStreams.toByteArray(getInstrumentation().getContext().getAssets().open("flora.png")));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return WireValue.of(image);
        }
        if (responseType.is(MediaType.WEBP)) {
            EncodedImage image;
            try {
                image = EncodedImage.jpeg(ByteStreams.toByteArray(getInstrumentation().getContext().getAssets().open("flora.webp")));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return WireValue.of(image);
        }

        // default: plain text
        return WireValue.of(randomString(r, 128));
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
            case 0: {
                int m = r.nextInt(16);
                byte[] bytes = new byte[m];
                r.nextBytes(bytes);
                return new JsonPrimitive(HexBytes.toString(bytes));
            }
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

    private static byte[] randomBytes(Random r, int m) {
        int n = r.nextInt(2 * m);

        byte[] bytes = new byte[n];
        r.nextBytes(bytes);

        return bytes;
    }

    /** includes the full range of Unicode code points */
    private static String randomString(Random r, int m) {
        int n = r.nextInt(2 * m);

        char[] cs = new char[n];
        for (int i = 0; i < n; ++i) {
            cs[i] = (char) r.nextInt((int) Character.MAX_VALUE);
        }

        return new String(cs);
    }


}
