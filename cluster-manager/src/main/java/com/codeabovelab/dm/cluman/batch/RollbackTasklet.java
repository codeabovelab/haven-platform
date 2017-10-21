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

import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.job.JobComponent;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.model.ContainersManager;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Tasklet which remove new container if exists, extract old container info, check that it not exist and create it. <p/>
 * Note that we must evaluate approach of use this under "spring transaction" engine.
 */
@JobComponent
public class RollbackTasklet {

    @Autowired
    private JobContext context;

    @Autowired
    private StopContainerTasklet containerStopper;

    @Autowired
    private RemoveContainerTasklet containerRemover;

    @Autowired
    private CreateContainerTasklet containerCreator;

    @Autowired
    private NodesGroup nodesGroup;

    @Autowired
    private RollbackData rollbackData;

    public void rollback() {
        List<RollbackData.Record> records = rollbackData.getRecords();
        for(int i = records.size() - 1; i >= 0; --i) {
            RollbackData.Record record = records.get(i);
//possibly rollback error must be fatal?
//            try {
                switch (record.action) {
                    case CREATE:
                        removeCreated(record.container);
                        break;
                    case DELETE:
                        createDeleted(record.container);
                        break;
                    case STOP:
                        startStopped(record.container);
                        break;
                    default:
                        context.fire("Unknown action in {0}", record);
                }
//            } catch (Exception e) {
//                context.fire("Can not rollback {0}, due to error: {1}.", record, e);
//                LOG.error("Can not rollback {}, due to error.", record, e);
//            }
        }
    }

    private void removeCreated(ProcessedContainer pc) {
        context.fire("Try remove created \"{0}\"", pc);
        //first we must remove new container
        String name = pc.getName();
        ContainerDetails currcd = getContainers().getContainer(name);
        if(currcd == null) {
            context.fire("Container \"{0}\" is not exists, nothing to remove.", name);
            return;
        }
        final String currentImage = currcd.getImage();
        if(currentImage.equals(pc.getImage())) {
            ProcessedContainer trash = pc;
            if(!trash.getId().equals(currcd.getId())) {
                trash = fillBuilder(currcd, ProcessedContainer.builder()).build();
            }
            containerStopper.execute(trash);
            containerRemover.execute(trash);
        } else {
            context.fire("Can not remove because container \"{0}\" has unexpected image \"{1}\".", name, currentImage);
        }
    }

    private void createDeleted(ProcessedContainer pc) {
        context.fire("Try create removed \"{0}\"", pc);
        // we check that name of new container is not busy,
        String name = pc.getName();
        ContainerDetails oldcd = getContainers().getContainer(name);
        if(oldcd != null) {
            if(oldcd.getImage().equals(pc.getImage())) {
                // removed container is exists
                context.fire("Removed container \"{0}\" is exists, nothing to create.", name);
                return;
            }
            // we cat not create container with same name
            throw new IllegalStateException("Can not rollback because already has container with same name: " +
              oldcd.getName() +  " which had different with old image");
        }
        containerCreator.execute(pc);
    }

    private ProcessedContainer.Builder fillBuilder(ContainerDetails container, ProcessedContainer.Builder builder) {
        builder.id(container.getId());
        builder.node(container.getNode().getName());
        return builder;
    }

    private void startStopped(ProcessedContainer pc) {
        // usually container stop and then remove,
        // so at rollback it will created, and now expected as run
        context.fire("Try to start stopped \"{0}\"", pc);
        // we check that name of new container is not busy,
        String name = pc.getName();
        ContainerDetails oldcd = getContainers().getContainer(name);
        if(oldcd == null) {
            context.fire("Stopped container \"{0}\" is not exists, nothing to run.", name);
            return;
        }
        ServiceCallResult res = getContainers().startContainer(name);
        context.fire("Start container \"{0}\" with result code \"{1}\" and message \"{2}\" (id:{3})",
          pc.getName(), res.getCode(), res.getMessage(), oldcd.getId());
        BatchUtils.checkThatIsOkOrNotModified(res);
    }

    private ContainersManager getContainers() {
        return nodesGroup.getContainers();
    }
}
