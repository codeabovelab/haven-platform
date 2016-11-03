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

package com.codeabovelab.dm.cluman.batch;

import com.codeabovelab.dm.cluman.job.JobComponent;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.job.JobParam;
import com.codeabovelab.dm.cluman.ui.health.HealthCheckService;
import com.codeabovelab.dm.common.healthcheck.ServiceHealthCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Check health of docker container (not all container support this)
 */
@JobComponent
public class HealthCheckContainerTasklet {

    /**
     * Name of job parameter which is enable health check
     */
    public static final String JP_HEALTH_CHECK_ENABLED = "HealthCheckContainerProcessor.enabled";
    public static final String JP_HEALTH_CHECK_TIMEOUT = "HealthCheckContainerProcessor.timeout";
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value("${dm.batch.HealthCheckContainerProcessor.timeout:240000}")
    private long defaultTimeout;

    @Autowired
    private HealthCheckService healthCheckService;

    @Autowired
    private JobContext context;

    @JobParam(JP_HEALTH_CHECK_ENABLED)
    private boolean enabled;

    @JobParam(JP_HEALTH_CHECK_TIMEOUT)
    private long timeout;

    private final int tries = 3;

    public boolean execute(ProcessedContainer item) {
        if(!enabled) {
            return true;
        }
        String id = item.getId();
        long timeout = this.timeout;
        if(timeout < 0) {
            timeout = defaultTimeout;
        }
        boolean healthy = false;
        long tryTimeout = timeout / tries;
        for(int i = 0; i < tries && !healthy; ++i) {
            healthy = check(item, tryTimeout);
        }
        context.fire("Health check result \"{0}\" is {1} (id:{2})", item.getName(), healthy ? "good" : "bad", id);
        return healthy;
    }

    private boolean check(ProcessedContainer item, long timeout) {
        boolean healthy;
        String id = item.getId();
        try {
            ServiceHealthCheckResult res = healthCheckService.checkContainer(item.getCluster(), id, timeout);
            healthy = res != null && res.isHealthy();
        } catch (Exception e) {
            log.error("Exception on health check of {}", id, e);
            context.fire("Health check failed with error: {0}", e.toString());
            healthy = false;
        }
        return healthy;
    }
}
