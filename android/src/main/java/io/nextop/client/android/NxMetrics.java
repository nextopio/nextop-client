package io.nextop.client.android;

public class NxMetrics {

    Aggregate[] nets;
    // these slice the metrics by messages that contain each tag
    // for debugging, can compare messages with one tag versus messages with another
    // if there are n tags, this is n^2, each tag has the "does not contain" tags included for each tag
    Aggregate[] tagSlices;


    static abstract class Aggregate {
        long fromTimeMs;
        long toTimeMs;

        long sampleSize;

        T tags;
        // does not contain these tags; count is how many got excluded, e.g. overlap size
        T notTags;
        Q timeToDelivery;
        C failesMessages;
        C pendingMessages;
        Q bytesUsed;
        Q nonControlBytesUsed;
        Q powerUsers;
    }

    public static final class Q {

        // these are usually from reservoir sampling
        int[] percentiles;
        float[] percentileValues;
    }

    public static final class C {
        long count;
    }

    public static final class T {
        String[] tags;
        long[] counts;
    }


}
