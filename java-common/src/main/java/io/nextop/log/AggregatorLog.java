package io.nextop.log;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

// aggregates logged values and periodically dumps a summary
// each key stays active for some time, then is evicted and prints a summary at time of eviction
// the state of all active keys can be dumped at any time
public final class AggregatorLog extends DefaultLog {


    final Scheduler scheduler;
    final Scheduler.Worker worker;

    // aggregate config
    final int metricReservoirSize = 16;
    final int[] metricPercentiles = new int[]{5, 50, 95};
    final int metricWindowSize = 4;
    // a total count is also maintained
    final int[] countWindowsMs = new int[]{5000, 60000};

    int summaryIntervalMs = (int) TimeUnit.SECONDS.toMillis(5);
    int ejectTimeoutMs = (int) TimeUnit.SECONDS.toMillis(180);

    // process state
    // "process" is vacuum + print modified aggregates since last process
    @Nullable
    Subscription processSubscription = null;
    long mostRecentProcessNanos = Long.MAX_VALUE;
    long nextProcessNanos = Long.MAX_VALUE;

    final Object aggregatorStateMutex = new Object();
    // front is most recently updated; back is least recently updated
    final NavigableSet<Aggregator> orderedAggregators;
    final Map<AggregatorKey, Aggregator> aggregators;


    public AggregatorLog(Out out, Scheduler scheduler) {
        super(out);
        this.scheduler = scheduler;
        this.worker = scheduler.createWorker();

        orderedAggregators = new TreeSet<Aggregator>(C_UPDATE_PRIORITY);
        aggregators = new HashMap<AggregatorKey, Aggregator>(32);
    }


    /////// Log OVERRIDES ///////

    @Override
    public void count(Level level, String key, final long d) {
        update(level, new AggregatorKey(AggregatorType.PERCENTILE, key), new Action1<Aggregator>() {
            @Override
            public void call(Aggregator aggregator) {
                ((Count) aggregator).add(d);
            }
        });
    }

    @Override
    public void metric(Level level, String key, final long value, final Object unit) {
        final Unit u = Unit.valueOf(unit);
        update(level, new AggregatorKey(AggregatorType.PERCENTILE, key), new Action1<Aggregator>() {
            @Override
            public void call(Aggregator aggregator) {
                ((Percentile) aggregator).add(value, u);
            }
        });
    }


    /////// Summary ///////

    private void process() {
        synchronized (aggregatorStateMutex) {
            long nanos = System.nanoTime();

            // 1. scan summary
            // 2. scan ejection
            // 3. schedule next

            // 1.
            for (Iterator<Aggregator> itr = orderedAggregators.iterator(); itr.hasNext(); ) {
                Aggregator aggregator = itr.next();
                // else don't break because the locking order in #update may cause some slight mis-ordering
                // e.g. the pendingSummary bit might not be set even though the aggregator set was updated
                // break on time
                int k = 2;
                if (TimeUnit.MILLISECONDS.toNanos(k * summaryIntervalMs) < nanos - aggregator.mostRecentUpdateNanos) {
                    break;
                } else {
                    synchronized (aggregator) {
                        if (aggregator.pendingSummary) {
                            aggregator.pendingSummary = false;
                            aggregator.summarize();
                        }
                    }
                }
            }

            // 2.
            // in the eject scan, this is the stopping value
            long maxNonEjectionNanos = nanos + TimeUnit.MILLISECONDS.toNanos(ejectTimeoutMs) / 2;
            for (Iterator<Aggregator> itr = orderedAggregators.descendingIterator(); itr.hasNext(); ) {
                Aggregator aggregator = itr.next();
                if (TimeUnit.MILLISECONDS.toNanos(ejectTimeoutMs) < nanos - aggregator.mostRecentUpdateNanos) {
                    synchronized (aggregator) {
                        aggregator.ejected = true;
                        aggregator.eject();
                    }
                } else {
                    maxNonEjectionNanos = aggregator.mostRecentUpdateNanos;
                    break;
                }
            }

            // 3.
            mostRecentProcessNanos = nanos;
            if (null != processSubscription) {
                processSubscription.unsubscribe();
            }
            nextProcessNanos = maxNonEjectionNanos;
            worker.schedule(new Action0() {
                @Override
                public void call() {
                    process();
                }
            }, nextProcessNanos - nanos, TimeUnit.NANOSECONDS);
        }
    }


    /////// Aggregators ///////

    private void update(Level level, AggregatorKey key, Action1<Aggregator> updater) {
        Aggregator aggregator;
        synchronized (aggregatorStateMutex) {
            long nanos = System.nanoTime();

            aggregator = aggregators.get(key);
            if (null != aggregator) {
                orderedAggregators.remove(aggregator);
                aggregator.mostRecentUpdateNanos = nanos;
                orderedAggregators.add(aggregator);
            } else {
                switch (key.type) {
                    case COUNT:
                        aggregator = new Count(key, nanos);
                        break;
                    case PERCENTILE:
                        aggregator = new Percentile(key, nanos);
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                aggregators.put(key, aggregator);
                orderedAggregators.add(aggregator);
            }

            // check the process schedule
            if (nanos + summaryIntervalMs < nextProcessNanos) {
                if (null != processSubscription) {
                    processSubscription.unsubscribe();
                }
                processSubscription = worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        process();
                    }
                }, summaryIntervalMs, TimeUnit.MILLISECONDS);
            }
        }
        // rare timing issue due to locking order
        // if the aggregator lock were inside the aggregatorStateMutex, this wouldn't happen
        // but that would increase contention
        boolean ejected;
        synchronized (aggregator) {
            ejected = aggregator.ejected;
            if (!ejected) {
                ejected = false;
                aggregator.pendingSummary = true;
                aggregator.level = level;
                updater.call(aggregator);
            }
        }
        if (ejected) {
            update(level, key, updater);
        }
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



    enum AggregatorType {
        COUNT,
        PERCENTILE
    }

    static final class AggregatorKey implements Comparable<AggregatorKey> {
        final AggregatorType type;
        final String key;

        AggregatorKey(AggregatorType type, String key) {
            this.type = type;
            this.key = key;
        }

        @Override
        public int hashCode() {
            int c = type.hashCode();
            c = 31 * c + key.hashCode();
            return c;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof AggregatorKey)) {
                return false;
            }
            AggregatorKey b = (AggregatorKey) o;
            return type.equals(b.type) && key.equals(b.key);
        }

        @Override
        public int compareTo(AggregatorKey b) {
            int d = type.compareTo(b.type);
            if (0 != d) {
                return d;
            }
            d = key.compareTo(b.key);
            return d;
        }
    }


    // all state read/write under a lock on this object
    // except mostRecentUpdateNanos (see notes)
    abstract class Aggregator {
        final AggregatorKey key;

        boolean pendingSummary = false;
        boolean ejected = false;

        Level level = Level.INFO;


        // read/write under the aggregatorStateMutex
        long mostRecentUpdateNanos;


        Aggregator(AggregatorKey key, long nanos) {
            this.key = key;
            mostRecentUpdateNanos = nanos;
        }


        void summarize() {

        }

        void eject() {

        }
    }



    // FIXME count aggregation should be a rolling window, where there is a total and total in the last n updates
    // FIXME if write upstream, output single metrics. e.g. p50 key is key.p50 for a single value



    class Count extends Aggregator {


        Count(AggregatorKey key, long nanos) {
            super(key, nanos);
        }


        synchronized void add(long d) {

        }
    }

    class Percentile extends Aggregator {

        final long[] reservoir = new long[metricReservoirSize];
        // circular, "count" is head+1
        final long[] mostRecent = new long[metricWindowSize];
        final long[] mostRecentNanos = new long[metricWindowSize];
        int count = 0;

        // pinned to the first input
        @Nullable
        Unit unit = null;


        Percentile(AggregatorKey key, long nanos) {
            super(key, nanos);
        }


        synchronized void add(long value, Unit unit) {
            long cvalue;
            if (null == this.unit) {
                this.unit = unit;
                cvalue = value;
            } else {
                cvalue = this.unit.convert(value, unit);
            }

            long nanos = System.nanoTime();

        }



    }



}
