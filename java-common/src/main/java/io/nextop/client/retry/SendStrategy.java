package io.nextop.client.retry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/** Functional interface for send retry.
 * Strategies are immutable and can be shared, used across multiple threads. */
public interface SendStrategy {

    // FIXME clarify usage of this interface
    // FIXME - retry
    // FIXME - isSend()
    // FIXME   - if true, getDelay

    // FIXME rename onRetry
    SendStrategy retry();
    boolean isSend();
    // FIXME rename getSendDelay
    long getDelay(TimeUnit timeUnit);




    // common strategies

    SendStrategy INDEFINITE = new Builder().init(0, TimeUnit.MILLISECONDS
    ).withUniformRandom(1, TimeUnit.SECONDS
    ).repeat(5
    ).withUniformRandom(10, TimeUnit.SECONDS
    ).repeatIndefinitely(
    ).build();

    SendStrategy NO_RETRY = new Builder().init(0, TimeUnit.MILLISECONDS
    ).repeat(0
    ).build();




    public static final class Builder {
        private Node head = new Node();
        private List<Node> sequence = new ArrayList<Node>(4);


        public Builder() {
        }


        public Builder init(long delay, TimeUnit timeUnit) {
            head.type = Node.Type.UNIFORM;
            head.initMs = timeUnit.toMillis(delay);
            return this;
        }
        public Builder max(long delay, TimeUnit timeUnit) {
            head.maxMs = timeUnit.toMillis(delay);
            return this;
        }
        public Builder withUniformRandom(long delay, TimeUnit timeUnit) {
            head.type = Node.Type.UNIFORM_RANDOM;
            head.delayMs = timeUnit.toMillis(delay);
            return this;
        }
        public Builder withLinearRandom(long delayPerStep, TimeUnit timeUnit) {
            head.type = Node.Type.LINEAR_RANDOM;
            head.delayMs = timeUnit.toMillis(delayPerStep);
            return this;
        }
        public Builder withExponentialRandom(float factorPerStep) {
            head.type = Node.Type.LINEAR_RANDOM;
            head.factor = factorPerStep;
            return this;
        }
        public Builder repeat(int count) {
            if (count < 0) {
                throw new IllegalArgumentException();
            }
            head.count = count;
            sequence.add(new Node(head));
            head.count = 0;
            return this;
        }
        public Builder repeatIndefinitely() {
            // FIXME
            return repeat(Integer.MAX_VALUE);
        }
        public SendStrategy build() {
            if (sequence.isEmpty()) {
                throw new IllegalStateException();
            }

            // build in reverse
            @Nullable SendStrategy tail = null;
            for (int i = sequence.size() - 1; 0 <= i; --i) {
                tail = create(sequence.get(i), tail);
            }

            return tail;
        }





        SendStrategy create(Node node, @Nullable SendStrategy after) {
            switch (node.type) {
                case UNIFORM:
                    return new UniformSendStrategy(node.initMs, node.maxMs, node.count, after);
                case UNIFORM_RANDOM:
                    return new UniformRandomSendStrategy(node.initMs, node.maxMs, node.count, after,
                             node.delayMs, RandomSequence.create(new Random()));
                case LINEAR_RANDOM:
                    return new LinearRandomSendStrategy(node.initMs, node.maxMs, node.count, after,
                             node.delayMs, RandomSequence.create(new Random()));
                case EXPONENTIAL_RANDOM:
                    return new ExponentialRandomSendStrategy(node.initMs, node.maxMs, node.count, after,
                             node.factor, RandomSequence.create(new Random()));
                default:
                    throw new IllegalArgumentException();
            }
        }

        private static final class Node {
            static enum Type {
                UNIFORM,
                UNIFORM_RANDOM,
                LINEAR_RANDOM,
                EXPONENTIAL_RANDOM
            }

            Type type;
            long initMs;
            long maxMs;
            long delayMs;
            float factor;
            int count;

            Node() {
                type = Type.UNIFORM;
                initMs = 0;
                maxMs = -1;
                delayMs = 0;
                factor = 1.f;
                count = 0;
            }

            Node(Node copy) {
                type = copy.type;
                initMs = copy.initMs;
                maxMs = copy.maxMs;
                delayMs = copy.delayMs;
                factor = copy.factor;
                count = copy.count;
            }


        }



        private static abstract class AbstractSendStrategy implements SendStrategy {
            int countDown;
            long initMs;
            long maxMs;
            @Nullable
            SendStrategy after;


            AbstractSendStrategy(long initMs, long maxMs, int countDown, @Nullable SendStrategy after) {
                this.initMs = initMs;
                this.maxMs = maxMs;
                this.countDown = countDown;
                this.after = after;
            }

            @Override
            public boolean isSend() {
                return 0 < countDown;
            }
        }

        private static final class UniformSendStrategy extends AbstractSendStrategy {
            UniformSendStrategy(long initMs, long maxMs, int countDown, @Nullable SendStrategy after) {
                super(initMs, maxMs, countDown, after);
            }

            @Override
            public long getDelay(TimeUnit timeUnit) {
                return Math.min(TimeUnit.MILLISECONDS.convert(initMs, timeUnit),
                        TimeUnit.MILLISECONDS.convert(maxMs, timeUnit));
            }

            @Override
            public SendStrategy retry() {
                if (0 < countDown) {
                    return new UniformSendStrategy(initMs, maxMs, countDown - 1, after);
                } else {
                    return after;
                }
            }
        }
        private static final class UniformRandomSendStrategy extends AbstractSendStrategy {
            long delayMs;
            RandomSequence r;

            UniformRandomSendStrategy(long initMs, long maxMs, int countDown, @Nullable SendStrategy after,
                                      long delayMs, RandomSequence r) {
                super(initMs, maxMs, countDown, after);
                this.delayMs = delayMs;
                this.r = r;
            }

            @Override
            public long getDelay(TimeUnit timeUnit) {
                return Math.min(TimeUnit.MILLISECONDS.convert(initMs + Math.round(r.floatValue() * delayMs), timeUnit),
                        TimeUnit.MILLISECONDS.convert(maxMs, timeUnit));
            }

            @Override
            public SendStrategy retry() {
                if (1 < countDown) {
                    return new UniformRandomSendStrategy(initMs,
                            maxMs, countDown - 1, after, delayMs, r);
                } else {
                    return after;
                }
            }
        }
        private static final class LinearRandomSendStrategy extends AbstractSendStrategy {
            long delayMs;
            RandomSequence r;

            LinearRandomSendStrategy(long initMs, long maxMs, int countDown, @Nullable SendStrategy after,
                                      long delayMs, RandomSequence r) {
                super(initMs, maxMs, countDown, after);
                this.delayMs = delayMs;
                this.r = r;
            }

            @Override
            public long getDelay(TimeUnit timeUnit) {
                return Math.min(TimeUnit.MILLISECONDS.convert(initMs + Math.round(r.floatValue() * delayMs), timeUnit),
                        TimeUnit.MILLISECONDS.convert(maxMs, timeUnit));
            }

            @Override
            public SendStrategy retry() {
                if (1 < countDown) {
                    return new LinearRandomSendStrategy(initMs + Math.round(r.floatValue() * delayMs),
                            maxMs, countDown - 1, after, delayMs, r);
                } else {
                    return after;
                }
            }
        }
        private static final class ExponentialRandomSendStrategy extends AbstractSendStrategy {
            float factor;
            RandomSequence r;

            ExponentialRandomSendStrategy(long initMs, long maxMs, int countDown, @Nullable SendStrategy after,
                                     float factor, RandomSequence r) {
                super(initMs, maxMs, countDown, after);
                this.factor = factor;
                this.r = r;
            }

            @Override
            public long getDelay(TimeUnit timeUnit) {
                return Math.min(TimeUnit.MILLISECONDS.convert(Math.round(r.floatValue() * initMs * factor), timeUnit),
                        TimeUnit.MILLISECONDS.convert(maxMs, timeUnit));
            }

            @Override
            public SendStrategy retry() {
                if (1 < countDown) {
                    return new ExponentialRandomSendStrategy(Math.round(r.floatValue() * initMs * factor),
                            maxMs, countDown - 1, after, factor, r);
                } else {
                    return after;
                }
            }
        }


    }


}
