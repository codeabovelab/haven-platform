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

package com.codeabovelab.dm.cluman.ui;

import com.codeabovelab.dm.cluman.job.*;
import com.codeabovelab.dm.cluman.ui.model.UiJob;
import com.codeabovelab.dm.cluman.ui.model.UiJobEvent;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.mb.Subscription;
import com.codeabovelab.dm.common.utils.ExecutorUtils;
import com.codeabovelab.dm.common.utils.Throwables;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 */
@RestController
@RequestMapping(value = "/ui/api/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class JobApi {

    private final JobsManager jobsManager;

    @RequestMapping(value = "/", method = GET)
    public List<UiJob> list() {
        LocalDateTime time = LocalDateTime.now().minusDays(1L);
        return jobsManager.getJobs().stream()
          .filter(ji -> ji.getInfo().getEndTime().isAfter(time))
          .sorted(Comparator.comparing(jil -> jil.getInfo().getStartTime()))
          .map(UiJob::toUi)
          .collect(Collectors.toList());
    }

    @RequestMapping(value = "/types-name/", method = GET)
    public Set<String> listTypes() {
        return jobsManager.getTypes();
    }

    /* we need to use regexp in var for match literals with multiple dots  */
    @RequestMapping(value = "/types/{type:.+}", method = GET)
    public JobDescription getType(@PathVariable("type") String type) {
        return jobsManager.getDescription(type);
    }

    @RequestMapping(value = "/{job:.*}", method = GET)
    public UiJob getJob(@PathVariable("job") String job) {
        JobInstance ji = jobsManager.getJob(job);
        ExtendedAssert.notFound(ji, "Job was not found by id: " + job);
        return UiJob.toUi(ji);
    }

    @RequestMapping(value = "/{job:.*}/log", method = GET)
    public List<UiJobEvent> getJobLog(@PathVariable("job") String job) {
        JobInstance ji = jobsManager.getJob(job);
        ExtendedAssert.notFound(ji, "Job was not found by id: " + job);
        return ji.getLog().stream().map(JobApi::toUi).collect(Collectors.toList());
    }

    @RequestMapping(value = "/{job:.*}", method = DELETE)
    public UiJob deleteJob(@PathVariable("job") String job) {
        JobInstance ji = jobsManager.getJob(job);
        ExtendedAssert.notFound(ji, "Job was not found by id: " + job);
        ji.cancel();
        jobsManager.deleteJob(job);
        return UiJob.toUi(ji);
    }

    @RequestMapping(value = "/{job:.*}/logStream", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseBodyEmitter getJobLogStream(@PathVariable("job") String job) {
        JobInstance ji = jobsManager.getJob(job);
        ExtendedAssert.notFound(ji, "Job was not found by id: " + job);
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(TimeUnit.MINUTES.toMillis(10L));
        JobEventConsumer consumer = new JobEventConsumer(this.jobsManager, emitter, ji);
        ji.atEnd().addListener(() -> {
            // it need for job which finish before request
            emitter.complete();
        }, ExecutorUtils.DIRECT);
        // TODO  we may want to consume history, also.
        Subscription subs = jobsManager.getSubscriptions().openSubscriptionOnKey(consumer, ji.getInfo());
        emitter.onCompletion(() -> {
            // Emitter not invoke this at client disconnect,
            //  may be it will be fix in future versions
            subs.close();
        });
        return emitter;
    }

    @RequestMapping(value = "/", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public UiJob newJob(@RequestBody JobParameters parameters) throws Exception {
        JobInstance jobInstance = jobsManager.create(parameters);
        //start and wait when it started
        jobInstance.start().get();
        return UiJob.toUi(jobInstance);
    }

    private static UiJobEvent toUi(JobEvent event) {
        String message = event.getMessage();
        Throwable exception = event.getException();
        if(exception != null) {
            String exceptionString = Throwables.printToString(exception);
            if(message == null) {
                message = exceptionString;
            } else {
                message += "\n" + exceptionString;
            }
        }
        return UiJobEvent.builder()
                .info(event.getInfo())
                .time(event.getTime())
                .message(message)
                .build();
    }

    @Slf4j
    public static class JobEventConsumer implements Consumer<JobEvent> {

        private final ResponseBodyEmitter emitter;
        private final JobInstance jobInstance;
        private final JobsManager jobsManager;

        public JobEventConsumer(JobsManager jobsManager, ResponseBodyEmitter emitter, JobInstance jobInstance) {
            this.jobsManager = jobsManager;
            this.emitter = emitter;
            this.jobInstance = jobInstance;
        }

        @Override
        public void accept(JobEvent event) {
            try {
                UiJobEvent uje = toUi(event);
                emitter.send(uje, MediaType.APPLICATION_JSON_UTF8);
                emitter.send("\n", MediaType.TEXT_PLAIN);
            } catch (IllegalStateException | IOException e) {
                //we assume that it mean client disconnect or other unrecoverable error
                close();
                log.error("Disconnect due to error: {}", e.toString());
            } catch (Exception e) {
                // stack traces for this error too noisy in log
                log.error("Can not send event: {}", e.toString());
            }
            try {
                if(event.getInfo().getStatus().isEnd()) {
                    close();
                }
            } catch (Exception e) {
                log.error("end event error", e);
            }
        }

        private void close() {
            emitter.complete();
            jobsManager.getSubscriptions().unsubscribe(this);
        }
    }
}
