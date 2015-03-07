package io.nextop.log;

import javax.annotation.Nullable;

// aggregates logged values and periodically dumps a summary
// each key stays active for some time, then is evicted and prints a summary at time of eviction
// the state of all active keys can be dumped at any time
public final class AggregatorLog implements Log {

    private final Out out;

    //
    final int metricReservoirSize = 16;
    final int[] metricPercentiles = new int[]{5, 50, 95};


    // updated key set
    // map of key  -> aggregate
    // linked set of items by update (oldest at front)



    abstract static class Aggregator {
        String key;
        long mostRecentUpdateNanos;
    }

    static class Count extends Aggregator {


        void update(long count) {

        }
    }

    static class Percentile extends Aggregator {

        final long[] reservoir;
        // circular, "count" is head+1
        final long[] mostRecent;
        int count;

        @Nullable
        Unit unit = null;





    }



}
