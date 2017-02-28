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
import org.springframework.util.Assert;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
/**
 * Task which may be reScheduled many times, to final execution.
 * <pre>
 *     RescheduledTask dt = ...;
 *     dt.schedule(1l, TimeUnit.MINUTES);//schedule for 1min
 *     dt.schedule(10l, TimeUnit.MINUTES);//reschedule for 11min
 *     Thread.sleep(...) // we wait, it executed...
 *     dt.schedule(10l, TimeUnit.MINUTES);//schedule for 10 min, not reshedule, because it executed
 * </pre>
 */
public class RescheduledTask implements AutoCloseable {

    @Data
    public static class Builder {
        private ScheduledExecutorService service;
        private Runnable runnable;
        /**
         * Maximal delay between first call of {@link RescheduledTask#schedule(long, TimeUnit)} and
         * execution of task. Without this, infinite call of 'schedule' (when timeout greater that time between calls)
         * can delay execution to heat death of universe.
         */
        private long maxDelay;

        public Builder service(ScheduledExecutorService service) {
            setService(service);
            return this;
        }

        public Builder runnable(Runnable runnable) {
            setRunnable(runnable);
            return this;
        }

        /**
         * Maximal delay between first call of {@link RescheduledTask#schedule(long, TimeUnit)} and
         * execution of task. Without this, infinite call of 'schedule' (when timeout greater that time between calls)
         * can delay execution to heat death of universe.
         * @param maxDelay - delay, any non positive value mean infinity delay
         * @return this
         */
        public Builder maxDelay(long maxDelay, TimeUnit timeUnit) {
            setMaxDelay(timeUnit.toMillis(maxDelay));
            return this;
        }

        public RescheduledTask build() {
            return new RescheduledTask(this);
        }
    }

    private final ScheduledExecutorService service;
    private final Runnable runnable;
    private final Object lock = new Object();
    private volatile ScheduledFuture<?> sf;
    private volatile long firstScheduleAfterExec;
    private final long maxDelay;
    private volatile boolean closed;

    private RescheduledTask(Builder builder) {
        this.service = builder.service;
        Assert.notNull(this.service, "service is null");
        this.runnable = builder.runnable;
        Assert.notNull(this.runnable, "runnable is null");
        this.maxDelay = builder.maxDelay;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void schedule(long timeout, TimeUnit timeUnit) {
        synchronized (lock) {
            if(closed) {
                return;
            }
            if(sf != null) {
                final long curr = System.currentTimeMillis();
                if(sf.isDone() && !sf.isCancelled()) {
                    firstScheduleAfterExec = curr;
                }
                if(maxDelay <= 0 || firstScheduleAfterExec <= 0 || (curr - firstScheduleAfterExec) < maxDelay) {
                    sf.cancel(false);
                }
            }
            sf = service.schedule(this::execute, timeout, timeUnit);
        }
    }

    private void execute() {
        //here we can add 'before' and 'after' handlers
        this.runnable.run();
    }

    @Override
    public void close() {
        ScheduledFuture<?> sf;
        synchronized (lock) {
            sf = this.sf;
            this.closed = true;
        }
        if(sf != null) {
            sf.cancel(true);
        }
    }
}
