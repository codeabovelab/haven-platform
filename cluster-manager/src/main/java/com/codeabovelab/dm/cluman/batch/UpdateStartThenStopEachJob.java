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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Start newly updated copy of exists container (with new name), stop old container,
 * then repeat for next container.
 */
@JobBean(UpdateContainersUtil.JOB_PREFIX + "startThenStopEach")
public class UpdateStartThenStopEachJob implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateStartThenStopEachJob.class);

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

    @Autowired
    private TargetVersionPredicate predicate;

    @Autowired
    private ContainerConfigTasklet containerConfig;

    @JobParam(BatchUtils.JP_ROLLBACK_ENABLE)
    private boolean rollbackEnable;

    @Override
    public void run() {
        List<ProcessedContainer> containers = loader.getContainers(predicate);
        boolean needRollback = false;
        ProcessedContainer curr = null;
        try {
            for(ProcessedContainer container: containers) {
                ProcessedContainer withConfig = containerConfig.process(container);
                ProcessedContainer newContainer = imageUpgrader.execute(withConfig);
                // we must reset name, due to conflicts
                newContainer = newContainer.makeCopy().name(null).build();

                ProcessedContainer createdContainer = containerCreator.execute(newContainer);
                if(healthchecker.execute(createdContainer)) {
                    containerStopper.execute(container);
                    containerRemover.execute(container);
                } else if(needRollback = this.rollbackEnable) {
                    break;
                }
            }
        } catch (Exception e) {
            needRollback = this.rollbackEnable;
            if(needRollback) {
                jobContext.fire("Error on container {0}, try rollback", curr);
                LOG.error("Error on container {}, try rollback", curr, e);
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
