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

import com.codeabovelab.dm.common.mb.ConditionalMessageBusWrapper;
import com.codeabovelab.dm.common.mb.ConditionalSubscriptions;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.mb.MessageBusImpl;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 */
@Component
public class JobsManagerImpl implements JobsManager, SmartLifecycle {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private Map<String, JobFactory> factories;
    private final MessageBus<JobEvent> bus;
    private final ConcurrentMap<JobParameters, JobInstance> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final ListableBeanFactory beanFactory;
    private final JobBeanDescriptionFactory descFactory;
    private final TaskScheduler scheduler;
    private boolean running;
    private final long jobLifetime;

    @Autowired
    public JobsManagerImpl(JobConfiguration.JobsManagerConfiguration configuration, ListableBeanFactory beanFactory, JobBeanDescriptionFactory descFactory) {
        this.descFactory = descFactory;
        this.beanFactory = beanFactory;
        this.bus = MessageBusImpl.builder(JobEvent.class, (s) ->
          new ConditionalMessageBusWrapper<JobEvent, JobEventCriteria>(s, (e) -> e, JobEventCriteriaImpl::matcher)
        )
          .id(JobEvent.BUS).build();
        if(log.isDebugEnabled()) {
            this.bus.subscribe((e) -> log.debug("Job event: {}", e));
        }
        this.jobLifetime = parseJobLifetime(configuration.getExecutedJobLifetime());
        this.executor = Executors.newCachedThreadPool(makeThreadFactory("executor"));
        this.scheduler = makeScheduler(configuration.getSchedulerPoolSize());
    }

    private long parseJobLifetime(String expr) {
        if(StringUtils.hasText(expr)) {
            try {
                Duration duration = Duration.parse(expr);
                return duration.getSeconds();
            } catch (Exception e) {
               log.warn("Can not parse: {} ", expr, e);
            }
        }
        return TimeUnit.DAYS.toSeconds(1L);
    }

    private ThreadFactory makeThreadFactory(String part) {
        return new ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat(getClass().getSimpleName() + "-" + part + "-%d")
          .build();
    }

    private TaskScheduler makeScheduler(int poolSize) {
        ThreadPoolTaskScheduler tpts = new ThreadPoolTaskScheduler();
        tpts.setThreadFactory(makeThreadFactory("scheduler"));
        if(poolSize < 1) {
            poolSize = 10;
        }
        tpts.setPoolSize(poolSize);
        tpts.initialize();
        return tpts;
    }

    @Scheduled(fixedRate = 60_000L)
    public void cleanJobs() {
        List<JobInstance> list = new ArrayList<>(this.jobs.values());
        // we remove jobs which has been ended at more than one day ago
        LocalDateTime last = LocalDateTime.now().minusSeconds(jobLifetime);
        for(JobInstance jobInstance: list) {
            if(last.isAfter(jobInstance.getInfo().getEndTime())) {
                this.jobs.remove(jobInstance.getJobContext().getParameters());
            }
        }
    }

    private Map<String, JobFactory> loadFactories(ListableBeanFactory beanFactory) {
        ImmutableMap.Builder<String, JobFactory> mb = ImmutableMap.builder();
        //load factory beans
        String[] factoryNames = beanFactory.getBeanNamesForType(JobFactory.class);
        for(String factoryName: factoryNames) {
            JobFactory factory = beanFactory.getBean(factoryName, JobFactory.class);
            if(factory == this) {
                // we do not want to load self
                continue;
            }
            Set<String> types = factory.getTypes();
            for(String type: types) {
                mb.put(type, factory);
            }
        }
        //load job beans
        String[] jobNames = beanFactory.getBeanNamesForAnnotation(JobBean.class);
        for(String jobName: jobNames) {
            Class<?> jobType = beanFactory.getType(jobName);
            JobDescription jd = descFactory.getFor(jobName);
            mb.put(jobName, new JobFactoryForBean(this, jobName, jobType, jd));
        }
        return mb.build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ConditionalSubscriptions<JobEvent, JobEventCriteria> getSubscriptions() {
        return (ConditionalSubscriptions<JobEvent, JobEventCriteria>) bus.asSubscriptions();
    }

    @Override
    public Collection<JobInstance> getJobs() {
        return Collections.unmodifiableCollection(jobs.values());
    }

    @Override
    public JobInstance getJob(String id) {
        return jobs.values().stream().filter(j -> j.getInfo().getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public JobInstance deleteJob(String id) {
        JobInstance job = getJob(id);
        return jobs.remove(job.getJobContext().getParameters());
    }

    @Override
    public JobInstance create(JobParameters parameters) {
        return jobs.computeIfAbsent(parameters, (params) -> {
            String type = parameters.getType();
            JobFactory jobFactory = getFactory(type);
            return jobFactory.create(parameters);
        });
    }

    private JobFactory getFactory(String type) {
        JobFactory jobFactory = factories.get(type);
        Assert.notNull(jobFactory, "Unknown job type: " + type);
        return jobFactory;
    }

    @Override
    public JobDescription getDescription(String jobType) {
        return getFactory(jobType).getDescription(jobType);
    }

    @Override
    public Set<String> getTypes() {
        return factories.keySet();
    }

    Future<?> execute(Runnable run) {
        return this.executor.submit(run);
    }

    ScheduledFuture<?> schedule(Runnable run, String schedule) {
        return this.scheduler.schedule(run, new CronTrigger(schedule));
    }

    ListableBeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        this.stop();
        callback.run();
    }

    @Override
    public void start() {
        // we can load factories only when all other beans is prepared
        try {
            this.factories = loadFactories(beanFactory);
        } catch (Exception e) {
            log.error("On load factories", e);
        }
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
        this.executor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    MessageBus<JobEvent> getBus() {
        return this.bus;
    }
}
