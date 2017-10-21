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

package com.codeabovelab.dm.cluman.batch;

import com.codeabovelab.dm.cluman.job.JobBean;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.job.JobParam;
import com.codeabovelab.dm.cluman.ui.update.UpdateContainersUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.ListIterator;

/**
 * Stop all containers, then make updated copy for each and start.
 */
@Slf4j
@JobBean(UpdateContainersUtil.JOB_PREFIX + "stopThenStartAll")
public class UpdateStopThenStartAllJob implements Runnable {

    @Autowired
    private LoadContainersOfImageTasklet loader;


    @Autowired
    private StopContainerTasklet containerStopper;

    @Autowired
    private RemoveContainerTasklet containerRemover;

    @Autowired
    private UpgradeImageVersionTasklet imageUpgrader;

    @Autowired
    private CreateContainerTasklet containerCreator;

    @Autowired
    private HealthCheckContainerTasklet healthchecker;

    @Autowired
    private RollbackTasklet rollbacker;

    @Autowired
    private JobContext jobContext;

    @JobParam(BatchUtils.JP_ROLLBACK_ENABLE)
    private boolean rollbackEnable;

    @Autowired
    private ContainerNeedUpdatedPredicate predicate;
    @Autowired
    private ContainerConfigTasklet containerConfig;

    @Override
    public void run() {
        List<ProcessedContainer> containers = loader.getContainers(predicate);
        boolean needRollback = false;
        ProcessedContainer curr = null;
        try {
            ListIterator<ProcessedContainer> i = containers.listIterator();
            while(i.hasNext()) {
                ProcessedContainer container = i.next();
                curr = container;
                container = containerConfig.process(container);
                i.set(container);//we replace old container with config
                containerStopper.execute(container);
                containerRemover.execute(container);
            }
            for(ProcessedContainer container: containers) {
                curr = container;
                ProcessedContainer newContainer = imageUpgrader.execute(container);
                curr = newContainer;
                ProcessedContainer createdContainer = containerCreator.execute(newContainer);
                if(!healthchecker.execute(createdContainer) && (needRollback = this.rollbackEnable)) {
                    break;
                }
            }
        } catch (Exception e) {
            needRollback = this.rollbackEnable;
            if(needRollback) {
                jobContext.fire("Error on container {0}, try rollback", curr);
                log.error("Error on container {}, try rollback", curr, e);
            } else {
                jobContext.fire("Error on container {0}", curr);
                throw e;
            }
        }
        if(needRollback) {
            rollbacker.rollback();
        }
    }
}
