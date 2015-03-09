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

    /* for counts and metrics, only the aggregates are written upstream. */

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


        abstract void summarize();
        abstract void eject();
    }


    // the windows in the counter hard reset on the window boundaries
    // there is no blending between windows
    class Count extends Aggregator {
        // initializes to zero
        final long[] windows = new long[countWindowsMs.length];
        final long[] windowStartNanos = new long[countWindowsMs.length];

        long total = 0L;
        long startNanos = 0L;
        int count = 0;

        Count(AggregatorKey key, long nanos) {
            super(key, nanos);
        }


        synchronized void add(long d) {
            long nanos = System.nanoTime();

            // update windows
            rotateWindows(nanos);
            for (int i = 0, n = countWindowsMs.length; i < n; ++i) {
                windows[i] += d;
            }

            // update total
            total += d;
            if (0 == count) {
                startNanos = nanos;
            }

            count += 1;
        }

        @Override
        void summarize() {
            summarize(key.key);
        }

        @Override
        void eject() {
            summarize(String.format("%s.eject", key.key));
        }

        // windows

        private void rotateWindows(long nanos) {
            int n = countWindowsMs.length;
            for (int i = 0; i < n; ++i) {
                if (TimeUnit.MILLISECONDS.toNanos(countWindowsMs[i]) < nanos - windowStartNanos[i]) {
                    // start a new window
                    windows[i] = 0L;
                    windowStartNanos[i] = nanos;
                }
            }
        }

        // summarization

        private synchronized void summarize(String key) {
            if (count <= 0) {
                assert false;
                return;
            }

            int n = countWindowsMs.length;
            long nanos = System.nanoTime();

            // ensure the windows are up current before reporting
            rotateWindows(nanos);

            // two-line formatting
            // e.g.
            // -0.9m   -3m        -20m
            // 2       8        ; 20
            if (out.isWrite(level, LogEntry.Type.COUNT)) {
                StringBuilder[] lines = new StringBuilder[2];
                for (int i = 0; i < 2; ++i) {
                    lines[i] = new StringBuilder(out.lineWidth());
                }
                lines[1].append(String.format("%-" + out.keyWidth() + "s ", key));
                pad(lines);

                String valueFormat = "%-" + out.valueWidth() + "d";
                String paddedValueFormat = valueFormat + " ";

                // windows
                for (int i = 0; i < n; ++i) {
                    long wvalue = windows[i];
                    long wnanos = windowStartNanos[i];
                    float mins = ((nanos - wnanos) / (1000 * 1000)) / (60 * 1000.f);

                    lines[0].append(String.format("-%.2fm ", mins));
                    lines[1].append(String.format(paddedValueFormat, wvalue));

                    pad(lines);
                }
                lines[1].append("; ");
                pad(lines);

                // total
                {
                    float mins = ((nanos - startNanos) / (1000 * 1000)) / (60 * 1000.f);
                    lines[0].append(String.format("-%.2fm ", mins));
                    lines[1].append(String.format(valueFormat, total));
                }

                String[] lineStrings = new String[2];
                for (int i = 0; i < 2; ++i) {
                    lineStrings[i] = lines[i].toString();
                }
                out.write(level, LogEntry.Type.COUNT, lineStrings);
            }

            // upstream keys are key.wX for each window ms, and a raw key with the total
            if (out.isWriteUp(level, LogEntry.Type.COUNT)) {
                out.writeUp(LogEntry.count(level, key, total));
                for (int i = 0; i < n; ++i) {
                    String wkey = String.format("%s.w%d", key, countWindowsMs[i]);
                    long wvalue = windows[i];
                    out.writeUp(LogEntry.count(level, wkey, wvalue));
                }
            }
        }
    }

    final class Percentile extends Aggregator {
        final Sample[] reservoir = new Sample[metricReservoirSize];
        // circular, "count" is head+1
        final Sample[] mostRecent = new Sample[metricWindowSize];
        int count = 0;

        // pinned to the first input
        @Nullable
        Unit unit = null;

        // TODO consider thread-local random to reduce memory usage
        final Random r = new Random();


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

            Sample s = new Sample(cvalue, nanos);
            // most recent
            {
                int i = count % mostRecent.length;
                mostRecent[i] = s;
            }
            // reservoir sampling for values
            {
                int i;
                if (count < reservoir.length) {
                    i = count;
                } else {
                    i = r.nextInt(count + 1);
                }
                if (i < reservoir.length) {
                    reservoir[i] = s;
                }
            }

            count += 1;
        }

        @Override
        synchronized void summarize() {
            summarize(key.key);
        }

        @Override
        synchronized void eject() {
            summarize(String.format("%s.eject", key.key));
        }


        // summarization

        private synchronized void summarize(String key) {
            if (count <= 0) {
                assert false;
                return;
            }

            // sort
            int k = Math.min(count, reservoir.length);
            Arrays.sort(reservoir, 0, k, C_SAMPLE_VALUE_ASCENDING);

            // calculate percentiles
            int n = metricPercentiles.length;
            int[] percentileIndexes = new int[n];
            for (int i = 0; i < n; ++i) {
                percentileIndexes[i] = ((10 * metricPercentiles[i] * (k - 1) + /* round */ 5) / 1000) / 10;
            }

            // three-line formatting
            // e.g.
            // p5      p50      p95      most recent
            // 2       8        16     ; [5       7        1     ] mbps
            // -.033m   -5.4m   -1.3m     -0.01m  -0.03m   -0.1m
            if (out.isWrite(level, LogEntry.Type.METRIC)) {
                long nanos = System.nanoTime();

                StringBuilder[] lines = new StringBuilder[3];
                for (int i = 0; i < 3; ++i) {
                    lines[i] = new StringBuilder(out.lineWidth());
                }
                lines[1].append(String.format("%-" + out.keyWidth() + "s ", key));
                pad(lines);

                String paddedValueFormat = "%-" + out.valueWidth() + "d ";

                // percentiles
                for (int i = 0; i < n; ++i) {
                    Sample s = reservoir[percentileIndexes[i]];
                    float mins = ((nanos - s.nanos) / (1000 * 1000)) / (60 * 1000.f);

                    lines[0].append(String.format("p%d ", metricPercentiles[i]));
                    lines[1].append(String.format(paddedValueFormat, s.value));
                    lines[2].append(String.format("-%.2fm ", mins));
                    pad(lines);
                }
                lines[1].append("; [");
                pad(lines);

                StringBuilder[] subLines = new StringBuilder[]{lines[1], lines[2]};
                int j = Math.min(count, mostRecent.length);
                for (int i = 0; i < j; ++i) {
                    int index = (count - 1 - i + mostRecent.length) % mostRecent.length;
                    Sample s = mostRecent[index];
                    float mins = ((nanos - s.nanos) / (1000 * 1000)) / (60 * 1000.f);
                    lines[1].append(String.format(paddedValueFormat, s.value));
                    lines[2].append(String.format("-%.2fm ", mins));
                    pad(subLines);
                }

                lines[0].append("most recent");
                lines[1].append("] ");
                lines[1].append(String.format("%" + out.unitWidth() + "s", unit));

                String[] lineStrings = new String[3];
                for (int i = 0; i < 3; ++i) {
                    lineStrings[i] = lines[i].toString();
                }
                out.write(level, LogEntry.Type.METRIC, lineStrings);
            }

            // upstream keys are key.pX for each percentile
            if (out.isWriteUp(level, LogEntry.Type.METRIC)) {
                for (int i = 0; i < n; ++i) {
                    String pkey = String.format("%s.p%d", key, metricPercentiles[i]);
                    long pvalue = reservoir[percentileIndexes[i]].value;
                    out.writeUp(LogEntry.metric(level, pkey, pvalue, unit));
                }
            }
        }
    }


    private static void pad(StringBuilder[] lines) {
        int length = 0;
        for (StringBuilder line : lines) {
            int n = line.length();
            if (length < n) {
                length = n;
            }
        }
        pad(lines, length);
    }
    private static void pad(StringBuilder[] lines, int length) {
        for (StringBuilder line : lines) {
            for (int d = length - line.length(); 0 < d; --d) {
                line.append(' ');
            }
        }
    }


    private static final class Sample {
        final long value;
        final long nanos;

        Sample(long value, long nanos) {
            this.value = value;
            this.nanos = nanos;
        }
    }

    private static final Comparator<Sample> C_SAMPLE_VALUE_ASCENDING = new Comparator<Sample>() {
        @Override
        public int compare(Sample a, Sample b) {
            if (a == b) {
                return 0;
            }
            // by value
            if (a.value < b.value) {
                return -1;
            }
            if (b.value < a.value) {
                return 1;
            }
            // by time
            if (a.nanos < b.nanos) {
                return -1;
            }
            if (b.nanos < a.nanos) {
                return 1;
            }
            return 0;
        }
    };
}
