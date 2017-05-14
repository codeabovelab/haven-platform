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

import com.codeabovelab.dm.cluman.model.CreateContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.CreateAndStartContainerResult;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ProcessEvent;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.configs.container.DefaultParser;
import com.codeabovelab.dm.cluman.job.JobComponent;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.job.JobParam;
import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.codeabovelab.dm.cluman.model.ImageName;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Processor which create containers
 */
@JobComponent
@Slf4j
public class CreateContainerTasklet {

    @Autowired
    private NodesGroup nodesGroup;

    @Autowired
    private JobContext context;

    @Autowired
    private RollbackData rollback;

    @Autowired
    private DefaultParser parser;

    @JobParam
    private Map<String, Object> containersConfigs;

    public ProcessedContainer execute(ProcessedContainer item) {
        ContainerSource cs = item.getSrc();
        createEnrichConfiguration(cs, item.getImage());
        cs.setName(item.getName());
        cs.setNode(item.getNode());
        cs.setCluster(item.getCluster());
        Assert.notNull(cs.getCluster(), "Cluster is null in " + item);
        cs.setImage(item.getImage());
        cs.setImageId(item.getImageId());
        context.fire("Create container \"{0}\" with \"{1}\" image on \"{2}\" node", cs.getName(), cs.getImage(), cs.getNode());
        CreateContainerArg arg = new CreateContainerArg()
                .enrichConfigs(true) // add to API
                .container(cs)
                .watcher(new MessageProxy());
        CreateAndStartContainerResult res = nodesGroup.getContainers().createContainer(arg);
        item = item.makeCopy().id(res.getContainerId()).name(res.getName()).build();
        rollback.record(item, RollbackData.Action.CREATE);
        ResultCode code = res.getCode();
        if (code != ResultCode.OK && code != ResultCode.NOT_MODIFIED) {
            throw new RuntimeException("On create " + arg + ", we got: " + res.getCode() + " " + res.getMessage());
        }
        return item;
    }

    @SuppressWarnings("unchecked")
    protected ContainerSource createEnrichConfiguration(ContainerSource arg, String image) {
        try {
            String registryAndImageName = ImageName.withoutTag(image);
            if (containersConfigs != null) {
                Object configs = containersConfigs.get(registryAndImageName);
                if (configs != null && configs instanceof Map) {
                    Map<String, Object> configsMap = (Map<String, Object>) configs;
                    parser.parse(configsMap, arg);
                }
            }
        } catch (Exception e) {
            log.error("error during enriching arg from config");
        }
        return arg;
    }

    private class MessageProxy implements Consumer<ProcessEvent> {
        @Override
        public void accept(ProcessEvent processEvent) {
            context.fire(processEvent.getMessage());
        }
    }
}
