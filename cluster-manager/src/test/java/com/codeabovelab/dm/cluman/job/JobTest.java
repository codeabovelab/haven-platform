package com.codeabovelab.dm.cluman.job;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import org.hamcrest.Matchers;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JobTest.JobTestConfiguration.class)
public class JobTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Configuration
    @Import(JobConfiguration.class)
    @ComponentScan(basePackageClasses = JobTest.class)
    public static class JobTestConfiguration {
    }

    @Autowired
    private JobsManager jobsManager;

    @Before
    public void before() {
        jobsManager.getSubscriptions().subscribe((e) -> {
            System.out.println(e);
        });
    }

    @Test
    public void test() throws Exception {
        Set<String> types = jobsManager.getTypes();
        log.warn("Types {}", types);
        final String jobName = "unnecessaryJobBean";
        assertThat(types, Matchers.hasItem(jobName));
        JobDescription description = jobsManager.getDescription(jobName);
        log.warn("JobDescription: {}", description);
        assertNotNull(description);
        final int intParam = 234;
        final String stringParam = "val";
        JobParameters params = JobParameters.builder()
          .type(jobName)
          .parameter("intParam", intParam)
          .parameter("stringParam", stringParam)
          .build();
        JobInstance ji = jobsManager.create(params);
        ListenableFuture<Boolean> start = ji.start();
        try {
            start.get(1L, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("On start ", e);
        }
        // wait at job end
        ji.atEnd().get(30L, TimeUnit.SECONDS);
        JobInfo info = ji.getInfo();
        log.warn("Info {}", info);
        assertEquals(JobStatus.COMPLETED, info.getStatus());
        JobContext ctx = ji.getJobContext();
        assertEquals(stringParam, ctx.getResult("stringParam"));
        assertEquals(intParam, ctx.getResult("intParam"));
        assertEquals(stringParam + intParam, ctx.getResult("ourJobResult"));
    }

    @Test
    public void testSchedule() throws Exception {
        final int intParam = 1;
        final String stringParam = "val";
        final int max = 9;
        final String jobName = "unnecessaryJobBean";
        JobParameters params = JobParameters.builder()
          .type(jobName)
          .schedule("*/1 * * * * *")
          .parameter("intParam", intParam)
          .parameter("stringParam", stringParam)
          .parameter("max", max)
          .build();
        JobInstance ji = jobsManager.create(params);
        ListenableFuture<Boolean> start = ji.start();
        try {
            start.get(1L, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("On start ", e);
        }
        // wait at job end
        //ji.atEnd().get(30l, TimeUnit.SECONDS);
        int seconds = 10;
        Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        JobInfo info = ji.getInfo();
        log.warn("Info {}", info);
        JobStatus status = info.getStatus();
        assertTrue(JobStatus.SCHEDULED == status || status == JobStatus.STARTED);
        JobContext ctx = ji.getJobContext();
        assertEquals(stringParam, ctx.getResult("stringParam"));
        assertEquals(max, ctx.getResult("intParam"));
        assertEquals(stringParam + max, ctx.getResult("ourJobResult"));
    }

    @Test
    public void testConcurrency() throws InterruptedException {
        final String jobName = "concurrentScheduleJob";
        final int concurrency = 10;
        List<JobInstance> jis = new ArrayList<>(concurrency);
        for(int i = 0; i < concurrency; ++i) {
            JobInstance ji = jobsManager.create(JobParameters.builder()
              .type(jobName)
              .schedule("*/1 * * * * *")
              .parameter("#", i)
              .build());
            jis.add(ji);
        }

        jis.stream().forEach(JobInstance::start);
        //*
        int seconds = 20;
        /*/
        int seconds = 1000000000;
        //*/
        Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        for(JobInstance ji: jis) {
            JobStatus status = ji.getInfo().getStatus();
            assertTrue(JobStatus.STARTED == status || JobStatus.SCHEDULED == status);
        }
        jis.stream().forEach(JobInstance::cancel);
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        for(JobInstance ji: jis) {
            JobStatus status = ji.getInfo().getStatus();
            assertEquals(JobStatus.CANCELLED, status);
        }
        assertEquals("Instances", concurrency, ConcurrentScheduleJob.getInstances());
        assertEquals("Conflicts", 0, ConcurrentScheduleJob.getConflicts());
        assertEquals("IterationComponentInstances", ConcurrentScheduleJob.getIterations(), IterationComponent.instances.get());
    }

    @Test
    public void testWatcher() throws Exception {
        final String jobName = "failingJob";
        JobInstance ji = jobsManager.create(JobParameters.builder()
              .type(jobName)
              .schedule("*/1 * * * * *")
              .build());
        ji.start().get(1L, TimeUnit.SECONDS);
        Thread.sleep(TimeUnit.SECONDS.toMillis(15L));
        JobStatus status = ji.getInfo().getStatus();
        assertEquals(JobStatus.CANCELLED, status);
        // default parameter of ScheduledJob
        assertEquals("Runnings", 10, FailingJob.getCount());
    }
}
