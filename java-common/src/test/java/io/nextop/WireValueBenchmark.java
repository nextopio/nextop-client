package io.nextop;

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class WireValueBenchmark {

    @Test
    public void testSize1() throws Exception {



        Random r = new Random();
        List<WireValue> list = new ArrayList<WireValue>(16);
        for (int i = 0; i < 16; ++i) {
            Map<WireValue, WireValue> m = new HashMap<WireValue, WireValue>(8);
            m.put(WireValue.of("test"), WireValue.of(r.nextInt()));
            m.put(WireValue.of("rest"), WireValue.of(r.nextInt()));
            list.add(WireValue.of(m));
        }

        WireValue value = WireValue.of(list);

        ByteBuffer bb = ByteBuffer.allocate(1024);
        value.toBytes(bb);
        bb.flip();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = new GZIPOutputStream(baos);

        WritableByteChannel channel = Channels.newChannel(gzos);
        channel.write(bb);
        gzos.finish();
        gzos.close();

        byte[] cbytes = baos.toByteArray();
        System.out.printf("c %d bytes\n", cbytes.length);



        String json = value.toString();
        System.out.printf("%s\n", json);
        System.out.printf("json %d bytes\n", json.length());
    }

    @Test
    public void testSize2() throws Exception {
        String json = "{\"speedups\":{\"1%\":7.45,\"2%\":17.55,\"3%\":26.64,\"4%\":28.99,\"5%\":36.74,\"6%\":43.69,\"7%\":46.24,\"8%\":47.87,\"9%\":55.63,\"10%\":63.8,\"11%\":71.45,\"12%\":72.88,\"13%\":77.48,\"14%\":83.8,\"15%\":89.83,\"16%\":92.28,\"17%\":101.87,\"18%\":109.43,\"19%\":111.47,\"20%\":118.41,\"21%\":120.04,\"22%\":120.15,\"23%\":122.29,\"24%\":127.9,\"25%\":136.38,\"26%\":137.5,\"27%\":144.44,\"28%\":147.4,\"29%\":153.83,\"30%\":163.74,\"31%\":164.14,\"32%\":164.76,\"33%\":165.27,\"34%\":171.49,\"35%\":174.96,\"36%\":181.19,\"37%\":184.87,\"38%\":190.89,\"39%\":192.52,\"40%\":200.59,\"41%\":205.08,\"42%\":212.63,\"43%\":216.41,\"44%\":220.49,\"45%\":223.15,\"46%\":227.95,\"47%\":237.54,\"48%\":242.75,\"49%\":249.79,\"50%\":253.67,\"51%\":259.28,\"52%\":264.49,\"53%\":267.55,\"54%\":269.59,\"55%\":277.56,\"56%\":284.29,\"57%\":287.36,\"58%\":290.42,\"59%\":300.22,\"60%\":309.71,\"61%\":311.96,\"62%\":315.63,\"63%\":317.37,\"64%\":324.62,\"65%\":324.62,\"66%\":326.35,\"67%\":328.7,\"68%\":330.03,\"69%\":337.48,\"70%\":347.48,\"71%\":354.83,\"72%\":356.36,\"73%\":357.49,\"74%\":358.51,\"75%\":364.74,\"76%\":372.09,\"77%\":381.07,\"78%\":386.99,\"79%\":391.79,\"80%\":400.26,\"81%\":407.41,\"82%\":407.61,\"83%\":411.59,\"84%\":416.19,\"85%\":418.84,\"86%\":418.94,\"87%\":426.9,\"88%\":432.21,\"89%\":442.32,\"90%\":448.95,\"91%\":453.04,\"92%\":461.92,\"93%\":468.86,\"94%\":473.25,\"95%\":476.41,\"96%\":480.8,\"97%\":486.42,\"98%\":487.85,\"99%\":493.97,\"100%\":500.0},\"messageVolumes\":{\"776968743862517 02.27.26591 19:04:22 UTC/776968743922517 02.27.26591 19:05:22 UTC\":{\"acked\":53,\"nacked\":36},\"776968743922517 02.27.26591 19:05:22 UTC/776968743982517 02.27.26591 19:06:22 UTC\":{\"acked\":20,\"nacked\":32},\"776968743982517 02.27.26591 19:06:22 UTC/776968744282517 02.27.26591 19:11:22 UTC\":{\"acked\":6,\"nacked\":103},\"776968744282517 02.27.26591 19:11:22 UTC/776968744582517 02.27.26591 19:16:22 UTC\":{\"acked\":133,\"nacked\":20},\"776968744582517 02.27.26591 19:16:22 UTC/776968746382517 02.27.26591 19:46:22 UTC\":{\"acked\":190,\"nacked\":282},\"776968746382517 02.27.26591 19:46:22 UTC/776968748182517 02.27.26591 20:16:22 UTC\":{\"acked\":49,\"nacked\":715},\"776968748182517 02.27.26591 20:16:22 UTC/776968751782517 02.27.26591 21:16:22 UTC\":{\"acked\":1059,\"nacked\":139},\"776968751782517 02.27.26591 21:16:22 UTC/776968794982517 02.28.26591 09:16:22 UTC\":{\"acked\":4507,\"nacked\":5937},\"776968794982517 02.28.26591 09:16:22 UTC/776968881382517 03.01.26591 09:16:22 UTC\":{\"acked\":1578,\"nacked\":6550},\"776968881382517 03.01.26591 09:16:22 UTC/776968967782517 03.02.26591 09:16:22 UTC\":{\"acked\":18855,\"nacked\":10082},\"776968967782517 03.02.26591 09:16:22 UTC/776969054182517 03.03.26591 09:16:22 UTC\":{\"acked\":14438,\"nacked\":15523},\"776969054182517 03.03.26591 09:16:22 UTC/776969140582517 03.04.26591 09:16:22 UTC\":{\"acked\":18164,\"nacked\":10294},\"776969140582517 03.04.26591 09:16:22 UTC/776969226982517 03.05.26591 09:16:22 UTC\":{\"acked\":5750,\"nacked\":23915},\"776969226982517 03.05.26591 09:16:22 UTC/776969313382517 03.06.26591 09:16:22 UTC\":{\"acked\":17540,\"nacked\":6600},\"776969313382517 03.06.26591 09:16:22 UTC/776969399782517 03.07.26591 09:16:22 UTC\":{\"acked\":23474,\"nacked\":3040},\"776969399782517 03.07.26591 09:16:22 UTC/776970004582517 03.14.26591 09:16:22 UTC\":{\"acked\":69159,\"nacked\":71601}},\"dataVolumes\":{\"776968743862517 02.27.26591 19:04:22 UTC/776968743922517 02.27.26591 19:05:22 UTC\":0.06986904,\"776968743922517 02.27.26591 19:05:22 UTC/776968743982517 02.27.26591 19:06:22 UTC\":0.03573799,\"776968743982517 02.27.26591 19:06:22 UTC/776968744282517 02.27.26591 19:11:22 UTC\":0.55406284,\"776968744282517 02.27.26591 19:11:22 UTC/776968744582517 02.27.26591 19:16:22 UTC\":0.04344654,\"776968744582517 02.27.26591 19:16:22 UTC/776968746382517 02.27.26591 19:46:22 UTC\":1.6460152,\"776968746382517 02.27.26591 19:46:22 UTC/776968748182517 02.27.26591 20:16:22 UTC\":3.8747225,\"776968748182517 02.27.26591 20:16:22 UTC/776968751782517 02.27.26591 21:16:22 UTC\":4.324559,\"776968751782517 02.27.26591 21:16:22 UTC/776968794982517 02.28.26591 09:16:22 UTC\":65.36603,\"776968794982517 02.28.26591 09:16:22 UTC/776968881382517 03.01.26591 09:16:22 UTC\":87.16559,\"776968881382517 03.01.26591 09:16:22 UTC/776968967782517 03.02.26591 09:16:22 UTC\":132.53012,\"776968967782517 03.02.26591 09:16:22 UTC/776969054182517 03.03.26591 09:16:22 UTC\":178.77545,\"776969054182517 03.03.26591 09:16:22 UTC/776969140582517 03.04.26591 09:16:22 UTC\":81.33208,\"776969140582517 03.04.26591 09:16:22 UTC/776969226982517 03.05.26591 09:16:22 UTC\":149.32678,\"776969226982517 03.05.26591 09:16:22 UTC/776969313382517 03.06.26591 09:16:22 UTC\":38.725124,\"776969313382517 03.06.26591 09:16:22 UTC/776969399782517 03.07.26591 09:16:22 UTC\":35.08357,\"776969399782517 03.07.26591 09:16:22 UTC/776970004582517 03.14.26591 09:16:22 UTC\":1276.8257}}";

        JsonElement e = new JsonParser().parse(json);

        WireValue value = WireValue.of(e);



        ByteBuffer bb = ByteBuffer.allocate(64 * 1024);
        value.toBytes(bb);
        bb.flip();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = new GZIPOutputStream(baos);

        WritableByteChannel channel = Channels.newChannel(gzos);
        channel.write(bb);
        gzos.finish();
        gzos.close();

        byte[] cbytes = baos.toByteArray();
        System.out.printf("c %d bytes\n", cbytes.length);



        System.out.printf("%s\n", json);
        System.out.printf("json %d bytes\n", json.length());
    }


    @Test
    public void testCompression1() throws Exception {



        Random r = new Random();

        byte[] dump = new byte[r.nextInt(1024)];
        r.nextBytes(dump);

        List<WireValue> list = new ArrayList<WireValue>(16);
        for (int i = 0; i < 16; ++i) {

            Map<WireValue, WireValue> m = new HashMap<WireValue, WireValue>(8);
            m.put(WireValue.of("test"), WireValue.of(r.nextInt()));
            m.put(WireValue.of("rest"), WireValue.of(r.nextInt()));
            m.put(WireValue.of("dump"), WireValue.of(dump));
            list.add(WireValue.of(m));
        }

        WireValue value = WireValue.of(list);

        ByteBuffer bb = ByteBuffer.allocate(64 * 1024);
        value.toBytes(bb);
        bb.flip();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = new GZIPOutputStream(baos);

        WritableByteChannel channel = Channels.newChannel(gzos);
        channel.write(bb);
        gzos.finish();
        gzos.close();

        byte[] cbytes = baos.toByteArray();
        System.out.printf("c %d bytes\n", cbytes.length);



        String json = value.toString();
        System.out.printf("%s\n", json);
        System.out.printf("json %d bytes\n", json.length());

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        GZIPOutputStream gzos2 = new GZIPOutputStream(baos2);
        gzos2.write(json.getBytes(Charsets.UTF_8));
        gzos2.finish();
        gzos2.close();
        byte[] jcbytes = baos2.toByteArray();
        System.out.printf("jsonc %d bytes\n", jcbytes.length);

    }

}
