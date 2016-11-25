/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.cluman.job;

/**
 * A "handle" which can trigger job rollback. <p/>
 * Note that this implementations must support serialization to json.
 * @see #rollbackParams(String)
 * @see RollbackJobBean
 */
public interface RollbackHandle {

    /**
     * Type of rollback job
     * @see #rollbackParams(String)
     */
    String ROLLBACK_JOB = "job.rollback";

    /**
     * Execute rollback in specified jobContext.
     */
    void rollback(RollbackContext context);

    /**
     * Make params object for run rollback job.
     * @param id
     * @return
     */
    static JobParameters.Builder rollbackParams(String id) {
        return JobParameters.builder()
          .type(RollbackHandle.ROLLBACK_JOB)
          .title("Rollback job: " + id)
          .parameter("jobId", id);
    }
}
