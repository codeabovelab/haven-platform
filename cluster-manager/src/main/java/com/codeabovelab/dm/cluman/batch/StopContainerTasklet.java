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

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.StopContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.job.JobComponent;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Container processor which is stop them
 */
@JobComponent
public class StopContainerTasklet {

    @Autowired
    private NodesGroup nodesGroup;

    @Autowired
    private JobContext context;

    @Autowired
    private RollbackData rollback;

    public void execute(ProcessedContainer item) {
        String id = item.getId();
        StopContainerArg arg = StopContainerArg.builder().id(id).build();
        rollback.record(item, RollbackData.Action.STOP);
        ServiceCallResult res = nodesGroup.getContainers().stopContainer(arg);
        context.fire("Stop container \"{0}\" with result code \"{1}\" and message \"{2}\" (id:{3})", item.getName(), res.getCode(), res.getMessage(), id);
        BatchUtils.checkThatIsOkOrNotModified(res);
    }
}
