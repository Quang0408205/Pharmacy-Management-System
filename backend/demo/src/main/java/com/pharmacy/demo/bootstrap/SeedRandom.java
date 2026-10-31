package com.pharmacy.demo.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SeedRandom {

    private final Random random;

    public SeedRandom(long seed) {
        this.random = new Random(seed);
    }

    public double next() {
        return random.nextDouble();
    }

    public int between(int minInclusive, int maxInclusive) {
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

    public boolean chance(double probability) {
        return random.nextDouble() < probability;
    }

    public <T> T pick(List<T> items) {
        return items.get(random.nextInt(items.size()));
    }

    @SafeVarargs
    public final <T> T pickOf(T... items) {
        return items[random.nextInt(items.length)];
    }

    public <T> List<T> shuffled(List<T> items) {
        List<T> copy = new ArrayList<>(items);
        Collections.shuffle(copy, random);
        return copy;
    }
}
