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

import com.google.common.base.MoreObjects;
import org.springframework.util.Assert;

import java.util.concurrent.ScheduledFuture;

/**
 */
class ScheduledJobInstanceImpl extends AbstractJobInstance {

    private final boolean repeatable;
    private volatile ScheduledFuture<?> scheduleHandle;

    public ScheduledJobInstanceImpl(Config config) {
        super(config);
        this.repeatable = config.isRepeatable();
        Assert.hasText(config.parameters.getSchedule(), "parameters.schedule is empty or null");
    }

    @Override
    protected boolean innerCancel() throws Exception {
        while(true) {
            JobStatus status = this.statusRef.get();
            if(status.isEnd()) {
                return false;
            }
            Boolean res = compareAndSetStatus(status, JobStatus.CANCELLED, () -> {
                cancel(this.scheduleHandle);
                cancel(this.executeHandle);
                // context not cleaned for if repeatable == true, therefore we need clean it there
                jobContext.close();
                return true;
            });
            if(res != null) {
                return res;
            }
        }
    }

    @Override
    protected boolean innerStart() throws Exception {
        Boolean res = compareAndSetStatus(JobStatus.CREATED, JobStatus.SCHEDULING, () -> {
            try {
                // we cannot schedule execution in another executor, because it hide real time of job execution and cause conflicts
                this.scheduleHandle = manager.schedule(new JobWrapper(this.job), this.getJobContext().getParameters().getSchedule());
                setStatus(JobStatus.SCHEDULED);
                return true;
            } catch (Throwable t) {
                fail(JobStatus.FAILED_JOB, t);
                throw t;
            }
        });
        return res != null && res;
    }

    @Override
    protected JobStatus completedStatus() {
        return JobStatus.SCHEDULED;
    }

    @Override
    protected JobStatus failedStatus() {
        return JobStatus.FAILED_STEP;
    }

    @Override
    protected void clearAfterIteration() {
        if(!repeatable) {
            jobContext.close();
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("scheduleHandle", scheduleHandle)
                .add("super", super.toString())
                .toString();
    }
}
