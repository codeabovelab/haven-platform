/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.common.utils;

import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Provide cache of single value.
 *
 */
public class SingleValueCache<T> implements Supplier<T> {

    public static class Builder<T> {
        private final Supplier<T> supplier;
        private long timeAfterWrite;

        Builder(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public Supplier<T> getSupplier() {
            return supplier;
        }

        public long getTimeAfterWrite() {
            return timeAfterWrite;
        }

        public Builder<T> timeAfterWrite(TimeUnit unit, long ttl) {
            setTimeAfterWrite(unit.toMillis(ttl));
            return this;
        }

        public Builder<T> timeAfterWrite(long ttl) {
            setTimeAfterWrite(ttl);
            return this;
        }

        public void setTimeAfterWrite(long timeAfterWrite) {
            this.timeAfterWrite = timeAfterWrite;
        }

        public SingleValueCache<T> build() {
            return new SingleValueCache<>(this);
        }
    }
    private final Supplier<T> supplier;
    private volatile T value;
    private volatile T oldValue;
    private volatile long writeTime;
    private final Lock lock = new ReentrantLock();
    private final long taw;

    private SingleValueCache(Builder<T> builder) {
        this.supplier = builder.supplier;
        this.taw = builder.timeAfterWrite;
    }

    public static <T> Builder<T> builder(Supplier<T> supplier) {
        return new Builder<>(supplier);
    }

    @Override
    public T get() {
        while(true) {
            if(lock.tryLock()) {
                try {
                    return loadIfNeed();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * Get previous value
     * @return previous value or null
     */
    public T getOldValue() {
        lock.lock();
        try {
            return oldValue;
        } finally {
            lock.unlock();
        }
    }

    private T loadIfNeed() {
        long time = System.currentTimeMillis();
        if(value != null && writeTime + taw >= time) {
            return value;
        }
        writeTime = time;
        oldValue = value;
        value = supplier.get();
        Assert.notNull(value, "Supplier '" + supplier + "' return null value");
        return value;
    }
}
