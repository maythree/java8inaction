package com.daou.chapter7;

import java.util.concurrent.atomic.AtomicLong;

public class Accumulator {
    public AtomicLong total = new AtomicLong(0);
    public void add(long value) {
        total.addAndGet(value);
    }
}

class Accumulator2 {
    public volatile long total = 0;
    public void add(long value) {
        total += value;
    }
}

class Accumulator3 {
    public static long total = 0;
    public void add(long value) {
        total += value;
    }
}

class Accumulator4 {
    public static volatile long total = 0;
    public void add(long value) {
        total += value;
    }
}
