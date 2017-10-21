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
import com.codeabovelab.dm.cluman.ui.update.UpdateContainersUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Stop each container, make updated clone with same name then start it, after repeat for next container.
 */
@JobBean(UpdateContainersUtil.JOB_PREFIX + "stopThenStartEach")
public class UpdateStopThenStartEachJob implements Runnable {

    @Autowired
    private StopThenStartEachStrategy startegy;

    @Autowired
    private UpgradeImageVersionTasklet upgrader;

    @Autowired
    private ContainerNeedUpdatedPredicate predicate;

    @Override
    public void run() {
        startegy.run(predicate, upgrader::execute);
    }
}
