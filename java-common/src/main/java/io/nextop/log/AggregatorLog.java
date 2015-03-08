package io.nextop.log;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;

// aggregates logged values and periodically dumps a summary
// each key stays active for some time, then is evicted and prints a summary at time of eviction
// the state of all active keys can be dumped at any time
public final class AggregatorLog extends DefaultLog {

    //
    final int metricReservoirSize = 16;
    final int[] metricPercentiles = new int[]{5, 50, 95};
    // a total count is also maintained
    final int[] countWindowsMs = new int[]{5000, 60000};


    Scheduler.Worker worker;

    // "process" is vacuum + print modified aggregates since last process
    @Nullable
    Subscription processSubscription = null;
    long mostRecentProcessNanos = 0L;


    // updated key set
    // map of key  -> aggregate
    // linked set of items by update (oldest at front)


    // front is most recently updated; back is least recently updated
    NavigableSet<Aggregator> orderedAggregators;
    Map<String, Aggregator> aggregators;


    // FIXME scheduler
    public AggregatorLog(Out out) {
        super(out);
    }

    


    /////// Log ///////

    @Override
    public void count(Level level, String key, long d) {
        // FIXME update
    }

    @Override
    public void metric(Level level, String key, long value, Object unit) {
        // FIXME update
    }


    /////// Aggregators ///////

    private void update(String key, Action1<Aggregator> updater) {
        // FIXME lock
        // FIXME set update nanos

    }


    static final Comparator<Aggregator> C_UPDATE_PRIORITY = new Comparator<Aggregator>() {
        @Override
        public int compare(Aggregator a, Aggregator b) {
            if (a == b) {
                return 0;
            }

            // descending by most recent update
            if (a.mostRecentUpdateNanos < b.mostRecentUpdateNanos) {
                return 1;
            } else if (b.mostRecentUpdateNanos < a.mostRecentUpdateNanos) {
                return -1;
            }

            return a.key.compareTo(b.key);
        }
    };



    abstract static class Aggregator {
        String key;
        long mostRecentUpdateNanos;
    }



    // FIXME count aggregation should be a rolling window, where there is a total and total in the last n updates
    // FIXME if write upstream, output single metrics. e.g. p50 key is key.p50 for a single value



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
