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

/**
 * Contract for DTO of heath check data entry
 * It can be healthy (with an optional message)
 * or unhealthy (with either an error message or a thrown exception).
 */
public interface HealthCheckResultData {

    /**
     * Id fo health data entry
     * @return id
     */
    String getId();

    /**
     * Additional message
     * @return message
     */
    String getMessage();

    /**
     * an exception thrown during the health check
     * @return an exception thrown during the health check
     */
    String getThrowable();

    /**
     * True value means healthy
     * @return healthy status
     */
    boolean isHealthy();
}
