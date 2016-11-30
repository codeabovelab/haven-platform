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

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Rollback any job which is support it.
 * @see RollbackHandle
 */
@JobBean(RollbackHandle.ROLLBACK_JOB)
public class RollbackJobBean implements Runnable {

    /**
     * Id of job which must be rollback
     */
    @JobParam(required = true)
    private String jobId;

    @Autowired
    private JobContext jobContext;

    @Autowired
    private ListableBeanFactory beanFactory;

    @Autowired
    private JobsManager jobsManager;

    @Override
    public void run() {
        JobInstance job = jobsManager.getJob(jobId);
        if(job == null) {
            throw new IllegalArgumentException("Can not find job for jobId:" + jobId);
        }
        JobContext jc = job.getJobContext();
        RollbackHandle rh = jc.getRollback();
        if(rh == null) {
            throw new IllegalArgumentException("Job (" + jobId + ") does not support rollback.");
        }
        RollbackContext rc = new RollbackContext(this.beanFactory, this.jobsManager, this.jobContext);
        rh.rollback(rc);
    }
}
