package io.nextop.client.retry;

import java.util.Random;

final class RandomSequence {
    static RandomSequence create(Random r) {
        int value;
        synchronized (r) {
            value = r.nextInt(Integer.MAX_VALUE);
        }
        return new RandomSequence(value, r);
    }


    private final int value;
    private final Random r;

    private RandomSequence(int value, Random r) {
        this.value = value;
        this.r = r;
    }

    public int intValue() {
        return value;
    }
    public float floatValue() {
        return (float) ((double) value / Integer.MAX_VALUE);
    }
    public RandomSequence next() {
        return create(r);
    }
}
