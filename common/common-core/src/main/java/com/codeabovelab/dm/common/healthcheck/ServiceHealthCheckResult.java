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

import com.codeabovelab.dm.common.log.ApplicationInfo;

import java.util.List;

/**
 * Health check command result contract
 */
public interface ServiceHealthCheckResult {

    /**
     * see com.codeabovelab.dm.common.log.ApplicationInfo
     * @return ApplicationInfo
     */
    ApplicationInfo getApplicationInfo();

    /**
     * see com.codeabovelab.dm.common.healthcheck.HealthCheckResultData
     * @return list of HealthCheckResultData
     */
    List<HealthCheckResultData> getResults();

    /**
     * health status
     * @return health status
     */
    boolean isHealthy();
}
