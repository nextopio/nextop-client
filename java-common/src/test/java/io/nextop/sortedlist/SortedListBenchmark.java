package io.nextop.sortedlist;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.Random;

/** Benchmarks a {@link SortedList} implementation
 * to verify lg performance of operations. */
public final class SortedListBenchmark<E> {
    static enum Op {
        INSERT,
        INSERT_ALL,
        GET,
        ITERATOR,
        LIST_ITERATOR,
        INDEX_OF,
        INDEX_OF_QUERY,
        LAST_INDEX_OF,
        LAST_INDEX_OF_QUERY,
        LOWER,
        LOWER_QUERY,
        LOWER_INDEX,
        LOWER_INDEX_QUERY,
        FLOOR,
        FLOOR_QUERY,
        FLOOR_INDEX,
        FLOOR_INDEX_QUERY,
        HIGHER,
        HIGHER_QUERY,
        HIGHER_INDEX,
        HIGHER_INDEX_QUERY,
        CEILING,
        CEILING_QUERY,
        CEILING_INDEX,
        CEILING_INDEX_QUERY
    }
    
    
    final String label;
    final Func1<Integer, SortedList<E>> sortedListGenerator;
    final Func1<Integer, E> elementGenerator;
    final Func1<Integer, Comparable<? super E>> queryGenerator;
    
    final Random r;
    
    final int tryCount = 256;
    final int repeatMeanCount = 32;
    final int minSize = 1024;
    final int stepCount = 11;
    
    SortedListBenchmark(Random r, String label,
            Func1<Integer, SortedList<E>> sortedListGenerator,
            Func1<Integer, E> elementGenerator,
            Func1<Integer, Comparable<? super E>> queryGenerator) {
        this.r = r;
        this.label = label;
        this.sortedListGenerator = sortedListGenerator;
        this.elementGenerator = elementGenerator;
        this.queryGenerator = queryGenerator;
    }
    
    void run(Op ... ops) {
        for (Op op : ops) {
            switch (op) {
                case GET:
                    verifyLg(op, new Func2<SortedList<E>, Integer, Integer>() {
                        @Override
                        public Integer call(SortedList<E> sortedList, Integer size) {
                            int n = 1 + r.nextInt(2 * (repeatMeanCount - 1));
                            for (int i = 0; i < n; ++i) {
                                sortedList.get(r.nextInt(size));
                            }
                            return n;
                        }
                    });
                    break;
                case INSERT:
                    verifyLg(op, new Func2<SortedList<E>, Integer, Integer>() {
                        @Override
                        public Integer call(SortedList<E> sortedList, Integer size) {
                            int n = 1 + r.nextInt(2 * (repeatMeanCount - 1));
                            for (int i = 0; i < n; ++i) {
                                sortedList.insert(elementGenerator.call(size));
                            }
                            return n;
                        }
                    });
                    break;
                default:
                    // TODO
                    break;
            }
        }
    }
    
    void verifyLg(Op op, Func2<SortedList<E>, Integer, Integer> opf) {
        // iterate from a list of min size ... max size, doubling size at each step
        // the mean op time should linearly increase at each step if the ops are lg
        
        // warmup
        for (int step = 0; step < stepCount; ++step) {
            int size = minSize << step;
            SortedList<E> sortedList = sortedListGenerator.call(size);
            // FIXME current implementation does not allow duplication; HACK
            size = sortedList.size();
            for (int i = 0; i < tryCount; ++i) {
                opf.call(sortedList, size);
            }
        }
        
        // bench
        double[] meanMicrosPerOp = new double[stepCount];
        for (int step = 0; step < stepCount; ++step) {
            int size = minSize << step;
            SortedList<E> sortedList = sortedListGenerator.call(size);
            // FIXME current implementation does not allow duplication; HACK
            size = sortedList.size();
            long netNanos = 0L;
            int netc = 0;
            for (int i = 0; i < tryCount; ++i) {
                long nanos = System.nanoTime();
                // return must be >= 1
                netc += opf.call(sortedList, size);
                netNanos += System.nanoTime() - nanos;
            }
            assert 0 < netc;
            double netMicros = netNanos / 1000.0;
            meanMicrosPerOp[step] = netMicros / netc;
            System.out.printf("%-20s %30s(%7d)  mean %.6fms  net %.6fms\n", label, op, size, 
                    meanMicrosPerOp[step] / 1000.0, netMicros / 1000.0);
        }
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        double[][] xs = new double[stepCount][1];
        for (int step = 0; step < stepCount; ++step) {
            xs[step][0] = step;
        }
        regression.newSampleData(meanMicrosPerOp, xs);
        double rsq = regression.calculateRSquared();
        System.out.printf("%-20s %30s  rsq %.6f\n", label, op, rsq);
    }
    
    
    public static void main(String[] in) {

        final Random r = new Random();
        final int m = 16;
        final Func1<Integer, Integer> elementGenerator = new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer size) {
                return r.nextInt(m * size);
            }
        };
        final Func1<Integer, Comparable<? super Integer>> queryGenerator = new Func1<Integer, Comparable<? super Integer>>() {
            @Override
            public Comparable<? super Integer> call(Integer size) {
                return r.nextInt(m * size);
            }
        };
        
        // splay sorted list
        new SortedListBenchmark<Integer>(r, "SplaySortedList", new Func1<Integer, SortedList<Integer>>() {
            @Override
            public SortedList<Integer> call(Integer size) {
                SplaySortedList<Integer> splaySortedList = new SplaySortedList<Integer>();
                for (int i = 0, n = size; i < n; ++i) {
                    splaySortedList.insert(elementGenerator.call(size));
                }
                return splaySortedList;
            }
        }, elementGenerator, queryGenerator).run(Op.values());
    }
}
