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

package com.codeabovelab.dm.cluman.ui.update;

import com.codeabovelab.dm.cluman.batch.BatchUtils;
import com.codeabovelab.dm.cluman.batch.HealthCheckContainerTasklet;
import com.codeabovelab.dm.cluman.batch.LoadContainersOfImageTasklet;
import com.codeabovelab.dm.cluman.job.JobInstance;
import com.codeabovelab.dm.cluman.job.JobParameters;
import com.codeabovelab.dm.cluman.job.JobsManager;
import com.codeabovelab.dm.cluman.ui.JobApi;
import com.codeabovelab.dm.cluman.ui.model.UiUpdateContainers;
import com.codeabovelab.dm.common.utils.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.concurrent.TimeUnit;

/**
 * We use another container because UiRestController is already to big, and need for refactoring
 */
@Slf4j
@RestController
@RequestMapping("/ui/api")
public class UiUpdateContainersController {

    private final JobsManager jobsManager;

    @Autowired
    public UiUpdateContainersController(JobsManager jobsManager) {
        this.jobsManager = jobsManager;
    }

    @RequestMapping(value = "/clusters/{cluster}/containers/update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseBodyEmitter update(@PathVariable("cluster") String cluster,
                                      @RequestBody UiUpdateContainers req) {
        log.info("got scale update request: {}", req);
        JobParameters params = createParametersString(cluster, req);

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(TimeUnit.MINUTES.toMillis(10L));
        JobInstance jobInstance = jobsManager.create(params);
        JobApi.JobEventConsumer consumer = new JobApi.JobEventConsumer(this.jobsManager, emitter, jobInstance);
        jobsManager.getSubscriptions().subscribeOnKey(consumer, jobInstance.getInfo());
        log.info("Try start job: {}", params);
        jobInstance.start();
        return emitter;
    }

    private JobParameters createParametersString(String cluster, UiUpdateContainers req) {
        JobParameters.Builder b = JobParameters.builder();
        b.type(UpdateContainersUtil.JOB_PREFIX + req.getStrategy());
        Float percentage = req.getPercentage();
        if(percentage != null) {
            b.parameter(LoadContainersOfImageTasklet.JP_PERCENTAGE, percentage);
        }
        b.parameter(BatchUtils.JP_CLUSTER, cluster);
        b.parameter(LoadContainersOfImageTasklet.JP_IMAGE, req.getService());
        b.parameter(BatchUtils.JP_IMAGE_TARGET_VERSION, req.getVersion());
        b.parameter(HealthCheckContainerTasklet.JP_HEALTH_CHECK_ENABLED, req.isHealthCheckEnabled());
        b.parameter(BatchUtils.JP_ROLLBACK_ENABLE, req.isRollbackEnabled());
        //we pass random id, instead job will be cached
        b.parameter("id", Uuids.liteRandom());
        return b.build();
    }

}
