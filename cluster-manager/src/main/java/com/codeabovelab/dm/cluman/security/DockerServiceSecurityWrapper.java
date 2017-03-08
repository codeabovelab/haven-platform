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

package com.codeabovelab.dm.cluman.security;

import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfig;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.*;
import com.codeabovelab.dm.cluman.cluster.docker.model.*;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.*;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import com.codeabovelab.dm.cluman.model.DockerServiceInfo;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import com.codeabovelab.dm.common.security.Action;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.Assert;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 */
public class DockerServiceSecurityWrapper implements DockerService {

    private final AccessContextFactory aclContextFactory;
    private final DockerService service;

    public DockerServiceSecurityWrapper(AccessContextFactory aclContextFactory, DockerService service) {
        this.aclContextFactory = aclContextFactory;
        this.service = service;
    }

    public void checkServiceAccess(Action action) {
        AccessContext context = aclContextFactory.getContext();
        checkServiceAccessInternal(context, action);
    }

    private void checkServiceAccessInternal(AccessContext context, Action action) {
        checkClusterAccess(context, action);
        String node = getNode();
        if(node != null) {
            boolean granted = context.isGranted(SecuredType.NODE.id(node), action);
            if(!granted) {
                throw new AccessDeniedException("Access to node docker service '" + node + "' with " + action + " is denied.");
            }
        }
    }

    public void checkClusterAccess(Action action) {
        AccessContext context = aclContextFactory.getContext();
        checkClusterAccess(context, action);
    }

    public void checkClusterAccess(AccessContext context, Action action) {
        Assert.notNull(action, "Action is null");
        String cluster = getCluster();
        if(cluster != null) {
            boolean granted = context.isGranted(SecuredType.CLUSTER.id(cluster), action);
            if(!granted) {
                throw new AccessDeniedException("Access to cluster docker service '" + cluster + "' with " + action + " is denied.");
            }
        }
    }

    public void checkContainerAccess(String id, Action action) {
        Assert.notNull(action, "Action is null");
        AccessContext context = aclContextFactory.getContext();
        checkServiceAccessInternal(context, action == Action.READ? Action.READ : Action.ALTER_INSIDE);
        boolean granted = context.isGranted(SecuredType.CONTAINER.id(id), action);
        if(!granted) {
            throw new AccessDeniedException("Access to container '" + id + "' with " + action + " is denied.");
        }
    }

    public void checkImageAccess(AccessContext context, String id, Action action) {
        Assert.notNull(action, "Action is null");
        checkServiceAccessInternal(context, action == Action.READ? Action.READ : Action.ALTER_INSIDE);
        boolean granted = context.isGranted(SecuredType.LOCAL_IMAGE.id(id), action);
        if(!granted) {
            throw new AccessDeniedException("Access to image '" + id + "' with " + action + " is denied.");
        }
    }

    public void checkNetworkAccess(String name, Action action) {
        Assert.notNull(action, "Action is null");
        AccessContext context = aclContextFactory.getContext();
        checkServiceAccessInternal(context, action == Action.READ? Action.READ : Action.ALTER_INSIDE);
        boolean granted = context.isGranted(SecuredType.NETWORK.id(name), action);
        if(!granted) {
            throw new AccessDeniedException("Access to image '" + name + "' with " + action + " is denied.");
        }
    }

    @Override
    public String getCluster() {
        return service.getCluster();
    }

    @Override
    public String getNode() {
        return service.getNode();
    }

    @Override
    public String getAddress() {
        return service.getAddress();
    }

    @Override
    public boolean isOnline() {
        return service.isOnline();
    }

    @Override
    public List<DockerContainer> getContainers(GetContainersArg arg) {
        AccessContext context = aclContextFactory.getContext();
        checkServiceAccessInternal(context, Action.READ);
        return service.getContainers(arg).stream().filter((img) -> {
            return context.isGranted(SecuredType.CONTAINER.id(img.getId()), Action.READ);
        }).collect(Collectors.toList());
    }

    @Override
    public ContainerDetails getContainer(String id) {
        checkContainerAccess(id, Action.READ);
        return service.getContainer(id);
    }

    @Override
    public ServiceCallResult getStatistics(GetStatisticsArg arg) {
        checkContainerAccess(arg.getId(), Action.READ);
        return service.getStatistics(arg);
    }

    @Override
    public DockerServiceInfo getInfo() {
        checkServiceAccess(Action.READ);
        return service.getInfo();
    }

    @Override
    public ServiceCallResult startContainer(String id) {
        checkContainerAccess(id, Action.EXECUTE);
        return service.startContainer(id);
    }

    @Override
    public ServiceCallResult pauseContainer(String id) {
        checkContainerAccess(id, Action.EXECUTE);
        return service.pauseContainer(id);
    }

    @Override
    public ServiceCallResult unpauseContainer(String id) {
        checkContainerAccess(id, Action.EXECUTE);
        return service.unpauseContainer(id);
    }

    @Override
    public ServiceCallResult stopContainer(StopContainerArg arg) {
        checkContainerAccess(arg.getId(), Action.EXECUTE);
        return service.stopContainer(arg);
    }

    @Override
    public ServiceCallResult getContainerLog(GetLogContainerArg arg) {
        checkContainerAccess(arg.getId(), Action.READ);
        return service.getContainerLog(arg);
    }

    @Override
    public ServiceCallResult subscribeToEvents(GetEventsArg arg) {
        checkServiceAccess(Action.READ);
        return service.subscribeToEvents(arg);
    }

    @Override
    public ServiceCallResult restartContainer(StopContainerArg arg) {
        checkContainerAccess(arg.getId(), Action.EXECUTE);
        return service.restartContainer(arg);
    }

    @Override
    public ServiceCallResult killContainer(KillContainerArg arg) {
        checkContainerAccess(arg.getId(), Action.DELETE);
        return service.killContainer(arg);
    }

    @Override
    public ServiceCallResult deleteContainer(DeleteContainerArg arg) {
        checkContainerAccess(arg.getId(), Action.DELETE);
        return service.deleteContainer(arg);
    }

    @Override
    public CreateContainerResponse createContainer(CreateContainerCmd cmd) {
        checkContainerAccess(null, Action.CREATE);
        return service.createContainer(cmd);
    }

    @Override
    public ServiceCallResult createTag(TagImageArg cmd) {
        checkImageAccess(aclContextFactory.getContext(), cmd.getImageName(), Action.UPDATE);
        return service.createTag(cmd);
    }

    @Override
    public ServiceCallResult updateContainer(UpdateContainerCmd cmd) {
        checkContainerAccess(cmd.getId(), Action.UPDATE);
        return service.updateContainer(cmd);
    }

    @Override
    public ServiceCallResult renameContainer(String id, String newName) {
        checkContainerAccess(id, Action.UPDATE);
        return service.renameContainer(id, newName);
    }

    @Override
    public CreateNetworkResponse createNetwork(CreateNetworkCmd cmd) {
        checkNetworkAccess(cmd.getName(), Action.CREATE);
        return service.createNetwork(cmd);
    }

    @Override
    public ServiceCallResult deleteNetwork(String id) {
        checkNetworkAccess(id, Action.DELETE);
        return service.deleteNetwork(id);
    }

    @Override
    public Network getNetwork(String id) {
        checkNetworkAccess(id, Action.READ);
        return service.getNetwork(id);
    }

    @Override
    public PruneNetworksResponse pruneNetworks(PruneNetworksArg arg) {
        checkServiceAccess(Action.ALTER_INSIDE);
        return service.pruneNetworks(arg);
    }

    @Override
    public ServiceCallResult connectNetwork(ConnectNetworkCmd cmd) {
        checkNetworkAccess(cmd.getNetwork(), Action.UPDATE);
        checkContainerAccess(cmd.getContainer(), Action.UPDATE);
        return service.connectNetwork(cmd);
    }

    @Override
    public ServiceCallResult disconnectNetwork(DisconnectNetworkCmd cmd) {
        checkNetworkAccess(cmd.getNetwork(), Action.UPDATE);
        checkContainerAccess(cmd.getContainer(), Action.UPDATE);
        return service.disconnectNetwork(cmd);
    }

    @Override
    public List<Network> getNetworks() {
        AccessContext context = aclContextFactory.getContext();
        checkServiceAccessInternal(context, Action.READ);
        return service.getNetworks().stream().filter((net) -> context.isGranted(SecuredType.NETWORK.id(net.getId()), Action.READ))
                .collect(Collectors.toList());
    }

    @Override
    public List<ImageItem> getImages(GetImagesArg arg) {
        AccessContext context = aclContextFactory.getContext();
        checkServiceAccessInternal(context, Action.READ);
        return  service.getImages(arg).stream().filter((img) -> {
            return context.isGranted(SecuredType.LOCAL_IMAGE.id(img.getId()), Action.READ);
        }).collect(Collectors.toList());
    }

    @Override
    public ImageDescriptor pullImage(String name, Consumer<ProcessEvent> watcher) {
        // here service can load image, but we cannot check access by name, and need check it by id after loading
        AccessContext context = aclContextFactory.getContext();
        checkServiceAccessInternal(context, Action.READ);
        ImageDescriptor image = service.pullImage(name, watcher);
        checkImageAccess(context, name, Action.READ);
        return image;
    }

    @Override
    public ImageDescriptor getImage(String name) {
        // here service can load image, but we cannot check access by name, and need check it by id after loading
        AccessContext context = aclContextFactory.getContext();
        checkServiceAccessInternal(context, Action.READ);
        ImageDescriptor image = service.getImage(name);
        checkImageAccess(context, name, Action.READ);
        return image;
    }

    @Override
    public ClusterConfig getClusterConfig() {
        checkServiceAccess(Action.READ);
        return service.getClusterConfig();
    }

    @Override
    public RemoveImageResult removeImage(RemoveImageArg arg) {
        checkImageAccess(aclContextFactory.getContext(), arg.getImageId(), Action.DELETE);
        return service.removeImage(arg);
    }

    @Override
    public SwarmInspectResponse getSwarm() {
        checkServiceAccess(Action.READ);
        return service.getSwarm();
    }

    @Override
    public SwarmInitResult initSwarm(SwarmInitCmd cmd) {
        checkServiceAccess(Action.UPDATE);
        return service.initSwarm(cmd);
    }

    @Override
    public ServiceCallResult joinSwarm(SwarmJoinCmd cmd) {
        checkServiceAccess(Action.UPDATE);
        return service.joinSwarm(cmd);
    }

    @Override
    public ServiceCallResult leaveSwarm(SwarmLeaveArg arg) {
        checkServiceAccess(Action.UPDATE);
        return service.leaveSwarm(arg);
    }

    @Override
    public List<SwarmNode> getNodes(GetNodesArg cmd) {
        checkServiceAccess(Action.READ);
        return service.getNodes(cmd);
    }

    @Override
    public ServiceCallResult removeNode(RemoveNodeArg arg) {
        checkServiceAccess(Action.ALTER_INSIDE);
        return service.removeNode(arg);
    }

    @Override
    public ServiceCallResult updateNode(UpdateNodeCmd cmd) {
        checkServiceAccess(Action.ALTER_INSIDE);
        return service.updateNode(cmd);
    }

    @Override
    public Service getService(String service) {
        checkServiceAccess(Action.READ);
        return this.service.getService(service);
    }

    @Override
    public ServiceCallResult deleteService(String service) {
        checkServiceAccess(Action.ALTER_INSIDE);
        return this.service.deleteService(service);
    }

    @Override
    public ServiceUpdateResult updateService(UpdateServiceArg arg) {
        checkServiceAccess(Action.ALTER_INSIDE);
        return service.updateService(arg);
    }

    @Override
    public ServiceCreateResult createService(CreateServiceArg arg) {
        checkServiceAccess(Action.ALTER_INSIDE);
        return service.createService(arg);
    }

    @Override
    public List<Service> getServices(GetServicesArg arg) {
        checkServiceAccess(Action.READ);
        return service.getServices(arg);
    }

    @Override
    public List<Task> getTasks(GetTasksArg arg) {
        checkServiceAccess(Action.READ);
        return service.getTasks(arg);
    }

    @Override
    public Task getTask(String taskId) {
        checkServiceAccess(Action.READ);
        return service.getTask(taskId);
    }

    @Override
    public List<Volume> getVolumes(GetVolumesArg arg) {
        checkServiceAccess(Action.READ);
        return service.getVolumes(arg);
    }

    @Override
    public Volume createVolume(CreateVolumeCmd cmd) {
        checkServiceAccess(Action.ALTER_INSIDE);
        return service.createVolume(cmd);
    }

    @Override
    public ServiceCallResult removeVolume(RemoveVolumeArg arg) {
        checkServiceAccess(Action.ALTER_INSIDE);
        return service.removeVolume(arg);
    }

    @Override
    public ServiceCallResult deleteUnusedVolumes(DeleteUnusedVolumesArg arg) {
        checkServiceAccess(Action.ALTER_INSIDE);
        return service.deleteUnusedVolumes(arg);
    }

    @Override
    public Volume getVolume(String name) {
        checkServiceAccess(Action.READ);
        return service.getVolume(name);
    }
}
