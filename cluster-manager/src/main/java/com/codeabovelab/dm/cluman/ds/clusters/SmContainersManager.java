/*
 * Copyright 2017 Code Above Lab LLC
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

package com.codeabovelab.dm.cluman.ds.clusters;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetTasksArg;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.ContainerSpec;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Endpoint;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Task;
import com.codeabovelab.dm.cluman.model.Port;
import com.codeabovelab.dm.cluman.model.ContainerService;
import com.codeabovelab.dm.cluman.model.ContainersManager;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import com.codeabovelab.dm.cluman.model.NodeInfo;
import com.codeabovelab.dm.common.utils.StringUtils;
import com.codeabovelab.dm.common.utils.TimeUtils;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Containers manager for swarm-mode clusters.
 */
@Slf4j
class SmContainersManager implements ContainersManager {
    public static final Joiner JOINER = Joiner.on(' ');
    protected final DockerCluster dc;

    SmContainersManager(DockerCluster dc) {
        this.dc = dc;
    }

    protected DockerService getDocker() {
        DockerService service = this.dc.getDocker();
        Assert.notNull(service, "Cluster return null docker value");
        return service;
    }


    @Override
    public Collection<ContainerService> getServices() {
        //TODO getDocker().getServices();
        return Collections.emptyList();
    }

    @Override
    public Collection<DockerContainer> getContainers() {
        List<Task> tasks = getDocker().getTasks(new GetTasksArg());
        ImmutableList.Builder<DockerContainer> builder = ImmutableList.builder();
        Map<String, NodeInfo> nodes = new HashMap<>();
        dc.getNodes().forEach(ni -> {
            String id = ni.getIdInCluster();
            if(id != null) {
                nodes.put(id, ni);
            }
        });
        tasks.forEach((t) -> {
            try {
                NodeInfo ni = nodes.get(t.getNodeId());
                DockerContainer dc = fromTask(ni, t);
                builder.add(dc);
            } catch (Exception e) {
                // full task.toString() too big for log
                log.error("On {}", t.getId(), e);
            }
        });
        return builder.build();
    }

    private DockerContainer fromTask(NodeInfo ni, Task task) {
        DockerContainer.Builder dcb = DockerContainer.builder();
        ContainerSpec container = task.getSpec().getContainer();
        String image = container.getImage();
        // TODO move into imagename utils
        dcb.setImage(StringUtils.before(image, '@'));
        dcb.setImageId(StringUtils.after(image, '@'));
        dcb.setName(task.getName());
        dcb.setId(task.getId());
        dcb.setNode(ni);
        dcb.setLabels(task.getLabels());
        List<String> command = container.getCommand();
        if(command != null) {
            dcb.setCommand(JOINER.join(command));
        }
        dcb.setCreated(TimeUtils.toMillis(task.getCreated()));
        Task.TaskStatus status = task.getStatus();
        Task.PortStatus portStatus = status.getPortStatus();
        if(portStatus != null) {
            List<Endpoint.PortConfig> ports = portStatus.getPorts();
            if(ports != null) {
                ports.forEach(pc -> {
                    dcb.getPorts().add(new Port(pc.getTargetPort(), pc.getPublishedPort(), pc.getProtocol()));
                });
            }
        }
        dcb.setState(convertState(status.getState()));
        dcb.setStatus(MoreObjects.firstNonNull(status.getError(), status.getMessage()));
        return dcb.build();
    }

    private DockerContainer.State convertState(Task.TaskState state) {
        switch (state) {
            case NEW:
            case ALLOCATED:
            case PENDING:
            case ASSIGNED:
            case ACCEPTED:
            case PREPARING:
            case READY:
            case STARTING:
                return DockerContainer.State.CREATED;
            case RUNNING:
                return DockerContainer.State.RUNNING;
            case COMPLETE:
            case SHUTDOWN:
                return DockerContainer.State.EXITED;
            case FAILED:
            case REJECTED:
                return DockerContainer.State.DEAD;
        }
        return null;
    }
}
