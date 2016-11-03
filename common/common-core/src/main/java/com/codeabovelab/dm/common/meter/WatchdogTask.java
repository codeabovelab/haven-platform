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

package com.codeabovelab.dm.common.meter;

import com.codahale.metrics.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The task for watchdog. <p/>
 * Only one task can correspond for each metric.
 */
public final class WatchdogTask {
    private static final Logger LOG = LoggerFactory.getLogger(WatchdogTask.class);

    private final Metric metric;
    private final String name;
    private final List<LimitChecker> limitCheckers = new CopyOnWriteArrayList<>();
    private final AtomicReference<ScheduledFuture<?>> scheduledRef = new AtomicReference<>();
    private final Runnable task = new WatchdogTaskRunnable(this);
    private final Watchdog watchdog;
    private volatile WatchdogTaskState state;

    WatchdogTask(Watchdog watchdog, Metric metric, String name) {
        this.watchdog = watchdog;
        Assert.hasText(name);
        Assert.notNull(metric);
        this.metric = metric;
        this.name = name;
        // stub state mean that all limits in normal state
        this.state = new WatchdogTaskState(name, System.currentTimeMillis(), Collections.<LimitExcess>emptyList());
    }

    /**
     * period between checks
     * @return
     */
    private long getPeriod() {
        // find minimal period
        long min = -1;
        for(LimitChecker limitChecker: limitCheckers) {
            long period = limitChecker.getPeriod();
            if(min == -1 || period < min) {
                min = period;
            }
        }
        return min;
    }

    void schedule() {
        cancel();
        //schedule
        long period = getPeriod();
        if(period > 0) {
            ScheduledFuture<?> scheduledFuture = this.watchdog.scheduledExecutorService.scheduleWithFixedDelay(task,
              period, period, TimeUnit.MILLISECONDS);
            if(!this.scheduledRef.compareAndSet(null, scheduledFuture)) {
                //something already scheduled
                scheduledFuture.cancel(true);
            }
        }
    }

    void cancel() {
        // cancel old schedule
        ScheduledFuture<?> oldScheduled = this.scheduledRef.getAndSet(null);
        if(oldScheduled != null) {
            oldScheduled.cancel(true);
        }
    }

    public void addLimitChecker(LimitChecker limitChecker) {
        //Assert.notNull(limitChecker);
        limitCheckers.add(limitChecker);
        schedule();
    }

    public void removeLimitChecker(LimitChecker limitChecker) {
        //Assert.notNull(limitChecker);
        limitCheckers.remove(limitChecker);
        schedule();
    }

    /**
     * state of watched limits.
     * @return state, never null
     */
    public WatchdogTaskState getState() {
        return state;
    }

    public Metric getMetric() {
        return metric;
    }

    /**
     * True if no limit checkers
     * @return
     */
    public boolean isEmpty() {
        return limitCheckers.isEmpty();
    }

    private static class WatchdogTaskRunnable implements Runnable {

        private final WatchdogTask watchdogTask;

        WatchdogTaskRunnable(WatchdogTask watchdogTask) {
            this.watchdogTask = watchdogTask;
        }

        @Override
        public void run() {
            List<LimitExcess> excesses = new ArrayList<>();
            LimitCheckContext limitCheckContext = new LimitCheckContext(watchdogTask.metric, watchdogTask.name);
            for(LimitChecker limitChecker: watchdogTask.limitCheckers) {
                LimitExcess excess = null;
                try {
                    excess = limitChecker.check(limitCheckContext);
                } catch (Exception e) {
                    LOG.error("Limit check error", e);
                }
                if(excess != null) {
                    excesses.add(excess);
                }
            }
            this.watchdogTask.updateState(limitCheckContext, excesses);
        }
    }

    private void updateState(LimitCheckContext context, List<LimitExcess> excesses) {
        final List<LimitExcess> unmodifiableList = Collections.unmodifiableList(excesses);
        this.state = new WatchdogTaskState(this.name, System.currentTimeMillis(), unmodifiableList);
        if(!excesses.isEmpty()) {
            LimitExcessEvent event = new LimitExcessEvent(context, unmodifiableList);
            this.watchdog.fireLimitExcess(event);
        }
    }
}
