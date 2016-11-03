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

package com.codeabovelab.dm.common.healthcheck;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.codeabovelab.dm.common.meter.MeterConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration which defines list of additional metrics for calculating health status
 */
@Configuration
@Import(MeterConfiguration.class)
public class HealthCheckConfiguration {

    @Autowired
    public void setHealthCheckRegistry(HealthCheckRegistry registry) {
        registry.register(ThreadDeadlockHealthCheck.class.getName(), new ThreadDeadlockHealthCheck());
    }

}
