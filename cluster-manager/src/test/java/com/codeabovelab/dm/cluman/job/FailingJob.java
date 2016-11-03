package com.codeabovelab.dm.cluman.job;

import lombok.ToString;

import java.util.concurrent.atomic.AtomicInteger;

/**
 */
@JobBean
@ToString
public class FailingJob implements Runnable {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Override
    public void run() {
        COUNTER.incrementAndGet();
        throw new RuntimeException("fail as planned");
    }

    public static int getCount() {
        return COUNTER.get();
    }
}
