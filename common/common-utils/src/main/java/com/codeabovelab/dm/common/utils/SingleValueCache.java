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

import lombok.Data;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Provide cache of single value.
 *
 */
public class SingleValueCache<T> implements Supplier<T> {

    public enum NullStrategy {
        /**
         * Deny null usage. Raise error when supplier return null. <p/>
         * Default behaviour.
         */
        DENY,
        /**
         * Handle null as usual value.
         */
        ALLOW,
        /**
         * Null value will interpreted as absent value, it not cached, and each {@link #get()} will try to load new value.
         */
        DIRTY
    }

    @Data
    public static class Builder<T> {
        private final Supplier<T> supplier;
        private long timeAfterWrite;
        private NullStrategy nullStrategy;

        Builder(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public Builder<T> timeAfterWrite(TimeUnit unit, long ttl) {
            setTimeAfterWrite(unit.toMillis(ttl));
            return this;
        }

        public Builder<T> timeAfterWrite(long ttl) {
            setTimeAfterWrite(ttl);
            return this;
        }

        public Builder<T> nullStrategy(NullStrategy nullStrategy) {
            setNullStrategy(nullStrategy);
            return this;
        }

        public SingleValueCache<T> build() {
            return new SingleValueCache<>(this);
        }
    }
    private static final Object NULL = new Object();
    private final Supplier<T> supplier;
    private volatile Object value;
    private volatile Object oldValue;
    private volatile long writeTime;
    private final Lock lock = new ReentrantLock();
    private final long taw;
    private final NullStrategy nullStrategy;

    private SingleValueCache(Builder<T> builder) {
        this.supplier = builder.supplier;
        this.taw = builder.timeAfterWrite;
        this.nullStrategy = builder.nullStrategy;
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
            return convert(oldValue);
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private T convert(Object value) {
        if(value == NULL) {
            return null;
        }
        return (T)value;
    }

    private T loadIfNeed() {
        long time = System.currentTimeMillis();
        if(value != null && writeTime + taw >= time) {
            return convert(value);
        }
        writeTime = time;
        oldValue = value;
        value = supplier.get();
        if(value == null) {
            switch (nullStrategy) {
                case ALLOW:
                    value = NULL;
                    break;
                case DIRTY:
                    break;
                case DENY:
                    throw new IllegalArgumentException("Supplier '" + supplier + "' return null value");
            }
        }
        return convert(value);
    }

    /**
     * Mark cache as invalid. Next call of {@link #get()} will load new value.
     */
    public void invalidate() {
        lock.lock();
        try {
            writeTime = 0L;
        } finally {
            lock.unlock();
        }
    }
}
