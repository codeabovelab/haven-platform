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
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

/**
 */
@Slf4j
class ScheduledJobInstanceImpl extends AbstractJobInstance {

    private final boolean repeatable;
    private volatile ScheduledFuture<?> scheduleHandle;

    public ScheduledJobInstanceImpl(Config config) {
        super(config);
        checkCronAndSetStartDate(config.getParameters().getSchedule());
        this.repeatable = config.isRepeatable();
    }

    private void checkCronAndSetStartDate(String schedule) {
        Assert.hasText(schedule, "parameters.schedule is empty or null");
        Assert.isTrue(CronSequenceGenerator.isValidExpression(schedule), "Cron expression is not valid: " + schedule);
        updateNextStart(calculateNextStart(schedule));
    }

    private void updateNextStart(LocalDateTime startTime) {
        JobInfo info = getInfo();
        setInfo(info, JobInfo.builder().from(info).startTime(startTime).build());
    }

    private LocalDateTime calculateNextStart(String schedule) {
        Date next = new CronSequenceGenerator(schedule).next(new Date());
        return LocalDateTime.ofInstant(next.toInstant(), ZoneId.systemDefault());
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
                JobScopeIteration.getBeans().close();
                JobScope.getBeans().close();
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
        JobScopeIteration.getBeans().close();
        if(!repeatable) {
            JobScope.getBeans().close();
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
