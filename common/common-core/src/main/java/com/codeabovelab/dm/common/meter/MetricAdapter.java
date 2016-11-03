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

import com.codahale.metrics.*;

import java.util.Map;

/**
 * adapter for specific metric
 */
interface MetricAdapter<T extends Metric> {

    MetricAdapter<Meter> METER = new MetricAdapter<Meter>() {
        @Override
        public Map<String, Meter> getMap(MetricRegistry metricRegistry) {
            return metricRegistry.getMeters();
        }

        @Override
        public Meter getOrCreate(MetricRegistry metricRegistry, String name) {
            return metricRegistry.meter(name);
        }
    };

    MetricAdapter<Counter> COUNTER = new MetricAdapter<Counter>() {
        @Override
        public Map<String, Counter> getMap(MetricRegistry metricRegistry) {
            return metricRegistry.getCounters();
        }

        @Override
        public Counter getOrCreate(MetricRegistry metricRegistry, String name) {
            return metricRegistry.counter(name);
        }
    };

    MetricAdapter<Timer> TIMER = new MetricAdapter<Timer>() {
        @Override
        public Map<String, Timer> getMap(MetricRegistry metricRegistry) {
            return metricRegistry.getTimers();
        }

        @Override
        public Timer getOrCreate(MetricRegistry metricRegistry, String name) {
            return metricRegistry.timer(name);
        }
    };

    Map<String, T> getMap(MetricRegistry metricRegistry);

    T getOrCreate(MetricRegistry metricRegistry, String name);
}
