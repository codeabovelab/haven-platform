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
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.cluster.docker.model.UpdateContainerCmd;
import com.codeabovelab.dm.cluman.ds.container.ContainerCreator;
import com.codeabovelab.dm.cluman.model.CreateContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.ContainerSpec;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Endpoint;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Service;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Task;
import com.codeabovelab.dm.cluman.ds.container.ContainerRegistration;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.common.utils.Functions;
import com.codeabovelab.dm.common.utils.SingleValueCache;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Containers manager for swarm-mode clusters. <p/>
 * We must prevent managing of containers which is enclosed to existed 'service'.
 */
@Slf4j
class DockerClusterContainers implements ContainersManager {
    public static final Joiner JOINER = Joiner.on(' ');
    public static final String LABEL_TASK_ID = "com.docker.swarm.task.id";
    public static final String LABEL_SERVICE_ID = "com.docker.swarm.service.id";
    protected final DockerCluster dc;
    protected final ContainerStorage containerStorage;
    protected final SingleValueCache<Map<String, ContainerService>> svcmap;
    private final ContainerCreator containerCreator;

    DockerClusterContainers(DockerCluster dc, ContainerStorage containerStorage, ContainerCreator containerCreator) {
        this.dc = dc;
        this.containerStorage = containerStorage;
        this.containerCreator = containerCreator;
        this.svcmap = SingleValueCache.builder(this::loadServices)
          .timeAfterWrite(TimeUnit.SECONDS, dc.getConfig().getConfig().getCacheTimeAfterWrite())
          .build();
    }

    private Map<String, ContainerService> loadServices() {
        List<Service> services = getDocker().getServices(new GetServicesArg());
        ImmutableMap.Builder<String, ContainerService> ilb = ImmutableMap.builder();
        services.forEach((s) -> {
            ContainerService.Builder csb = ContainerService.builder();
            csb.setCluster(dc.getName());
            csb.setCreated(s.getCreated());
            csb.setUpdated(s.getUpdated());
            csb.setId(s.getId());
            Service.ServiceSpec spec = s.getSpec();
            csb.setLabels(spec.getLabels());
            csb.setName(spec.getName());
            Task.TaskSpec task = spec.getTaskTemplate();
            ContainerSpec container = task.getContainer();
            String image = container.getImage();
            ImageName im = ImageName.parse(image);
            csb.setImage(im.getFullName());
            csb.setImageId(im.getId());
            csb.setCommand(container.getCommand());
            convertPorts(s.getEndpoint().getPorts(), csb.getPorts());
            ilb.put(s.getId(), csb.build());
        });
        Map<String, ContainerService> map = ilb.build();
        return map;
    }

    protected DockerService getDocker() {
        DockerService service = this.dc.getDocker();
        Assert.notNull(service, "Cluster return null docker value");
        return service;
    }

    @Override
    public Collection<ContainerService> getServices() {
        return svcmap.get().values();
    }

    private List<DockerContainer> getContainersInternal() {
        ImmutableList.Builder<DockerContainer> conts = ImmutableList.builder();
        List<ContainerRegistration> crs = containerStorage.getContainers();
        crs.forEach((cr) -> {
            DockerContainer container = cr.getContainer();
            if(this.dc.hasNode(container.getNode())) {
                conts.add(container);
            }
        });
        return conts.build();
    }

    @Override
    public Collection<DockerContainer> getContainers() {
        return getContainersInternal();
    }

    private DockerContainer.Builder fromTask(Task task, DockerContainer.Builder dcb) {
        ContainerSpec container = task.getSpec().getContainer();
        //dcb.setLabels(task.getLabels());
        List<String> command = container.getCommand();
        if(command != null) {
            dcb.setCommand(JOINER.join(command));
        }
        //dcb.setCreated(TimeUtils.toMillis(task.getCreated()));
        Task.TaskStatus status = task.getStatus();
        Task.PortStatus portStatus = status.getPortStatus();
        if(portStatus != null) {
            convertPorts(portStatus.getPorts(), dcb.getPorts());
        }
        dcb.setState(convertState(status.getState()));
        dcb.setStatus(MoreObjects.firstNonNull(status.getError(), status.getMessage()));
        return dcb;
    }

    private void convertPorts(List<Endpoint.PortConfig> ports, List<Port> target) {
        if(ports == null) {
            return;
        }
        ports.forEach(pc -> {
            target.add(new Port(pc.getTargetPort(), pc.getPublishedPort(), pc.getProtocol()));
        });
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

    @Override
    public ServiceCallResult createService(CreateServiceArg arg) {
        return getDocker().createService(arg);
    }

    @Override
    public ServiceCallResult updateService(UpdateServiceArg arg) {
        return getDocker().updateService(arg);
    }

    @Override
    public ServiceCallResult deleteService(String service) {
        return getDocker().deleteService(service);
    }

    @Override
    public CreateAndStartContainerResult createContainer(CreateContainerArg arg) {
        DockerService ds = getNodeForNew(arg);
        return containerCreator.createContainer(arg, ds);
    }

    private DockerService getNodeForNew(CreateContainerArg arg) {
        String node = arg.getContainer().getNode();
        if(node == null || !dc.hasNode(node)) {
            // we use dummy random strategy
            // from one side if you need good scheduling you must create 'service'
            // from other we support legacy contract when user can schedule container
            List<NodeInfo> nodes = dc.getNodes();
            int num = (int) (Math.random() * (nodes.size() - 1));
            NodeInfo nodeInfo = nodes.get(num);
            node = nodeInfo.getName();
        }
        return getNodeService(node);
    }

    private DockerService getNodeService(String node) {
        DockerService ds = dc.getNodeStorage().getNodeService(node);
        Assert.notNull(ds, "Can not find docker service for node: " + node);
        return ds;
    }

    private DockerService getContainerDocker(String containerId) {
        ContainerRegistration container = containerStorage.getContainer(containerId);
        return getNodeService(container.getNode());
    }

    @Override
    public ServiceCallResult updateContainer(EditContainerArg arg) {
        DockerService ds = getContainerDocker(arg.getContainerId());
        UpdateContainerCmd cmd = new UpdateContainerCmd();
        EditableContainerSource src = arg.getSource();
        cmd.from(src);
        return ds.updateContainer(cmd);
    }

    @Override
    public ServiceCallResult stopContainer(StopContainerArg arg) {
        DockerService ds = getContainerDocker(arg.getId());
        return ds.stopContainer(arg);
    }

    @Override
    public ServiceCallResult startContainer(String containerId) {
        DockerService ds = getContainerDocker(containerId);
        return ds.startContainer(containerId);
    }

    @Override
    public ServiceCallResult pauseContainer(String containerId) {
        DockerService ds = getContainerDocker(containerId);
        return ds.pauseContainer(containerId);
    }

    @Override
    public ServiceCallResult unpauseContainer(String containerId) {
        DockerService ds = getContainerDocker(containerId);
        return ds.unpauseContainer(containerId);
    }

    @Override
    public ServiceCallResult deleteContainer(DeleteContainerArg arg) {
        DockerService ds = getContainerDocker(arg.getId());
        return ds.deleteContainer(arg);
    }

    @Override
    public ServiceCallResult restartContainer(StopContainerArg arg) {
        DockerService ds = getContainerDocker(arg.getId());
        return ds.restartContainer(arg);
    }

    @Override
    public ServiceCallResult scaleContainer(ScaleContainerArg arg) {
        String containerId = arg.getContainerId();
        ContainerRegistration cr = containerStorage.getContainer(containerId);
        if(cr == null) {
            return new ServiceCallResult().code(ResultCode.NOT_FOUND).message(containerId + " is not registered");
        }
        String serviceId = cr.getContainer().getLabels().get(LABEL_SERVICE_ID);
        if(serviceId != null) {
            return scaleService(serviceId, arg.getScale());
        }
        throw new UnsupportedOperationException("Not implemented yet.");
        // we currently not support this because scale need strategy for spread containers between nodes
        // therefore user must create service, or in future we implement this
        //DockerService ds = getContainerDocker(arg.getContainerId());
        //containerCreator.scale(ds, arg.getScale(), arg.getContainerId());
    }

    @Override
    public ContainerDetails getContainer(String id) {
        DockerService ds = getContainerDocker(id);
        return ds.getContainer(id);
    }

    private ServiceCallResult scaleService(String serviceId, int scale) {
        DockerService docker = dc.getDocker();
        Service service = docker.getService(serviceId);
        Service.ServiceSpec origSpec = service.getSpec();
        UpdateServiceArg arg = new UpdateServiceArg();
        arg.setVersion(service.getVersion().getIndex());
        arg.setService(serviceId);
        Service.ServiceSpec.Builder ss = origSpec.toBuilder();
        ss.mode(Service.ServiceMode.builder().replicated(new Service.ReplicatedService(scale)).build());
        arg.setSpec(ss.build());
        return docker.updateService(arg);
    }
}
