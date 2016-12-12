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

import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 */
class JobFactoryForBean implements JobFactory {

    private final JobsManagerImpl jobManager;
    private final AtomicLong counter = new AtomicLong(0);
    private final Class<?> jobClass;
    private final String jobName;
    private final Set<String> types;
    private final JobDescription description;

    public JobFactoryForBean(JobsManagerImpl jobsManager, String jobName, Class<?> jobClass, JobDescription jobDescription) {
        this.jobManager = jobsManager;
        this.jobName = jobName;
        this.jobClass = jobClass;
        this.description = jobDescription;
        this.types = Collections.singleton(jobName);
    }

    @Override
    public JobInstance create(JobParameters parameters) {
        JobInfo.Builder infoBuilder = JobInfo.builder()
                .id(jobName + "-" + counter.getAndIncrement())
                .title(parameters.getTitle())
                .type(jobName)
                .createTime(LocalDateTime.now());

        JobBean ann = jobClass.getAnnotation(JobBean.class);
        boolean repeatable = ann.repeatable();
        AbstractJobInstance.Config config = new AbstractJobInstance.Config();
        config.setRepeatable(repeatable);
        config.setJob(new JobBeanTask(jobManager.getBeanFactory(), jobName));
        config.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
        config.setJobsManager(this.jobManager);
        config.setParameters(parameters);
        String schedule = parameters.getSchedule();
        if (StringUtils.hasText(schedule)) {
            Assert.isTrue(CronSequenceGenerator.isValidExpression(schedule), "Cron expression is not valid: " + schedule);
            Date next = new CronSequenceGenerator(parameters.getSchedule()).next(new Date());
            LocalDateTime startDate = LocalDateTime.ofInstant(next.toInstant(), ZoneId.systemDefault());
            infoBuilder.setStartTime(startDate);
            config.setInfo(infoBuilder.build());
            config.setWatcher(new ScheduledJobWatcher());
            return new ScheduledJobInstanceImpl(config);
        } else {
            config.setInfo(infoBuilder.build());
            return new JobInstanceImpl(config);
        }
    }

    @Override
    public JobDescription getDescription(String jobType) {
        return description;
    }

    @Override
    public Set<String> getTypes() {
        return types;
    }
}
