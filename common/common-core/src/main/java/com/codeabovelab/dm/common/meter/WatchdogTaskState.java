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

import java.util.List;

/**
 * immutable state of watchdog task
 */
public final class WatchdogTaskState {
    private final String metricId;
    private final long checkTime;
    private final List<LimitExcess> excesses;

    /**
     * state can be created only by watchdogtask
     * @param metricId
     * @param checkTime
     * @param excesses
     */
    WatchdogTaskState(String metricId, long checkTime, List<LimitExcess> excesses) {
        this.metricId = metricId;
        this.checkTime = checkTime;
        this.excesses = excesses;
    }

    /**
     * id of metric (also watchdog task)
     * @return
     */
    public String getMetricId() {
        return metricId;
    }

    /**
     * Time of state creation, state wil be created immediately after limits checking.
     * @return
     */
    public long getCheckTime() {
        return checkTime;
    }

    /**
     * List of exceeded limits. List may be empty, but non null.
     * @return
     */
    public List<LimitExcess> getExcesses() {
        return excesses;
    }
}
