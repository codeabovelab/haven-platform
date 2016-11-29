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

package com.codeabovelab.dm.cluman.job;

/**
 */
class JobInstanceImpl extends AbstractJobInstance {

    JobInstanceImpl(Config config) {
        super(config);
    }

    @Override
    protected boolean innerCancel() throws Exception {
        Boolean res = compareAndSetStatus(JobStatus.STARTED, JobStatus.CANCELLED, () -> {
            this.executeHandle.cancel(true);
            return true;
        });
        return res != null && res;
    }

    @Override
    protected boolean innerStart() throws Exception {
        Boolean res = compareAndSetStatus(JobStatus.CREATED, JobStatus.STARTING, () -> {
            try {
                this.executeHandle = manager.execute(new JobWrapper(this.job));
                setStatus(JobStatus.STARTED);
                return true;
            } catch (Throwable t) {
                setStatus(JobStatus.FAILED_JOB);
                throw t;
            }
        });
        return res != null && res;
    }

    @Override
    protected JobStatus completedStatus() {
        return JobStatus.COMPLETED;
    }

    @Override
    protected JobStatus failedStatus() {
        return JobStatus.FAILED_JOB;
    }

    @Override
    protected void clearAfterIteration() {
        JobScope.getBeans().close();
        JobScopeIteration.getBeans().close();
    }
}
