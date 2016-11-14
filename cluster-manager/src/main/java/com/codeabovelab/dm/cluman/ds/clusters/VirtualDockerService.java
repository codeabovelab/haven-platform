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

package com.codeabovelab.dm.cluman.ds.clusters;

import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfig;
import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ProcessEvent;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.RemoveImageResult;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.*;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.model.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 */
@Slf4j
class VirtualDockerService implements DockerService {

    private final NodesGroupImpl cluster;
    // we need empty config for prevent NPE
    private final ClusterConfig config = ClusterConfigImpl.builder()
      //workaround, possibly we must to create config runtime
      .addHost("<virtual host>")
      .build();

    VirtualDockerService(NodesGroupImpl cluster) {
        this.cluster = cluster;
    }

    @Override
    public ServiceCallResult createNetwork(CreateNetworkCmd createNetworkCmd) {
        //TODO
        return new ServiceCallResult().code(ResultCode.ERROR).message("Virtual cluster '" + getCluster() + "' does not support this.");
    }

    @Override
    public List<Network> getNetworks() {
        return Collections.emptyList();
    }

    @Override
    public String getCluster() {
        return this.cluster.getName();
    }

    @Override
    public String getNode() {
        return null;
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public List<DockerContainer> getContainers(GetContainersArg arg) {
        List<DockerContainer> virtConts = new ArrayList<>();
        for(Node node: cluster.getNodes()) {
            DockerService service = getServiceByNode(node);
            if(isOffline(service)) {
                // due to different causes service can be null
                continue;
            }
            try {
                List<DockerContainer> nodeContainer = service.getContainers(arg);
                virtConts.addAll(nodeContainer);
            } catch (AccessDeniedException e) {
                //nothing
            }
        }
        return virtConts;
    }

    @Override
    public List<ImageItem> getImages(GetImagesArg arg) {
        List<ImageItem> virt = new ArrayList<>();
        for(Node node: cluster.getNodes()) {
            DockerService service = getServiceByNode(node);
            if(isOffline(service)) {
                // due to different causes service can be null
                continue;
            }
            try {
                List<ImageItem> images = service.getImages(arg);
                virt.addAll(images);
            } catch (AccessDeniedException e) {
                //nothing
            }
        }
        return virt;
    }

    private DockerService getServiceByNode(Node node) {
        Assert.notNull(node, "Node is null");
        DockerServices dockerServices = this.cluster.getDockerServices();
        return  dockerServices.getNodeService(node.getName());
    }

    @Override
    public ContainerDetails getContainer(String id) {
        DockerService service = getServiceByContainer(id);
        if(isOffline(service)) {
            return null;
        }
        ContainerDetails container = service.getContainer(id);
        return container;
    }

    private DockerService getServiceByContainer(String id) {
        return this.cluster.getDockerServices().getServiceByContainer(id);
    }

    @Override
    public ServiceCallResult getStatistics(GetStatisticsArg arg) {
        String id = arg.getId();
        DockerService service = getServiceByContainer(id);
        if(isOffline(service)) {
            return whenNotFoundService(id);
        }
        return service.getStatistics(arg);
    }

    @Override
    public DockerServiceInfo getInfo() {
        List<NodeInfo> nodeList = new ArrayList<>();
        int containers = 0;
        int offContainers = 0;
        int offNodes = 0;
        for(Node node: cluster.getNodes()) {
            NodeInfo nodeInfo;
            if(node instanceof NodeInfo) {
                nodeInfo = (NodeInfo) node;
            } else {
                nodeInfo = this.cluster.getNodeStorage().getNodeInfo(node.getName());
            }
            if(nodeInfo != null) {
                nodeList.add(nodeInfo);
            }
            DockerService service = getServiceByNode(node);
            if(isOffline(service)) {
                offNodes++;
                // due to different causes service can be null
                continue;
            }
            if(nodeInfo == null || !nodeInfo.isOn()) {
                offNodes++;
            }
            try {
                List<DockerContainer> nodeContainer = service.getContainers(new GetContainersArg(true));
                int running = (int) nodeContainer.stream().filter(DockerContainer::isRun).count();
                containers += running;
                offContainers += nodeContainer.size() - running;
            } catch (AccessDeniedException e) {
                //nothing
            } catch (Exception e) {
                log.warn("Can not list containers on {}, due to error {}", node.getName(), e.toString());
            }
        }
        return DockerServiceInfo.builder()
          .name(getCluster())
          .nodeList(nodeList)
          .nodeCount(nodeList.size() - offNodes)
          .offNodeCount(offNodes)
          .containers(containers)
          .offContainers(offContainers)
          .build();
    }

    private boolean isOffline(DockerService service) {
        return service == null || !service.isOnline();
    }

    @Override
    public ServiceCallResult startContainer(String id) {
        DockerService service = getServiceByContainer(id);
        if(isOffline(service)) {
            return whenNotFoundService(id);
        }
        return service.startContainer(id);
    }

    @Override
    public ServiceCallResult stopContainer(StopContainerArg arg) {
        DockerService service = getServiceByContainer(arg.getId());
        if(isOffline(service)) {
            return whenNotFoundService(arg.getId());
        }
        return service.stopContainer(arg);
    }

    @Override
    public ServiceCallResult getContainerLog(GetLogContainerArg arg) {
        DockerService service = getServiceByContainer(arg.getId());
        if(isOffline(service)) {
            return whenNotFoundService(arg.getId());
        }
        return service.getContainerLog(arg);
    }

    @Override
    public ServiceCallResult subscribeToEvents(GetEventsArg arg) {
        throw new UnsupportedOperationException("Virtual cluster does not support.");
    }

    @Override
    public ServiceCallResult createTag(TagImageArg cmd) {
        throw new UnsupportedOperationException("Virtual cluster does not support.");
    }

    @Override
    public ServiceCallResult restartContainer(StopContainerArg arg) {
        DockerService service = getServiceByContainer(arg.getId());
        if(isOffline(service)) {
            return whenNotFoundService(arg.getId());
        }
        return service.restartContainer(arg);
    }

    @Override
    public ServiceCallResult killContainer(KillContainerArg arg) {
        DockerService service = getServiceByContainer(arg.getId());
        if(isOffline(service)) {
            return whenNotFoundService(arg.getId());
        }
        return service.killContainer(arg);
    }

    @Override
    public RemoveImageResult removeImage(RemoveImageArg removeImageArg) {
        RemoveImageResult removeImageResult = new RemoveImageResult();
        removeImageResult.code(ResultCode.OK);
        for(Node node: cluster.getNodes()) {
            DockerService service = getServiceByNode(node);
            if (isOffline(service)) {
                continue;
            }
            try {
                service.removeImage(removeImageArg);
            } catch (AccessDeniedException e) {
                //nothing
            }
        }
        return removeImageResult;
    }

    @Override
    public ServiceCallResult deleteContainer(DeleteContainerArg arg) {
        DockerService service = getServiceByContainer(arg.getId());
        if(isOffline(service)) {
            return whenNotFoundService(arg.getId());
        }
        return service.deleteContainer(arg);
    }

    private ServiceCallResult whenNotFoundService(String id) {
        return new ServiceCallResult().code(ResultCode.ERROR).message("Can not find service for container: " + id);
    }

    @Override
    public CreateContainerResponse createContainer(CreateContainerCmd cmd) {
        throw new UnsupportedOperationException("Virtual cluster does not support.");
    }

    @Override
    public ServiceCallResult updateContainer(UpdateContainerCmd cmd) {
        throw new UnsupportedOperationException("Virtual cluster does not support.");
    }

    @Override
    public ServiceCallResult renameContainer(String id, String newName) {
        throw new UnsupportedOperationException("Virtual cluster does not support.");
    }

    @Override
    public ImageDescriptor pullImage(String name, Consumer<ProcessEvent> watcher) {
        throw new UnsupportedOperationException("Virtual cluster does not support.");
    }

    @Override
    public ImageDescriptor getImage(String name) {
        ImageDescriptor image = null;
        for(Node node: cluster.getNodes()) {
            DockerService service = getServiceByNode(node);
            if(isOffline(service)) {
                // due to different causes service can be null
                continue;
            }
            try {
                image = service.getImage(name);
                if (image != null) {
                    break;
                }
            } catch (AccessDeniedException e) {
                //nothing
            }
        }
        return image;
    }

    public ClusterConfig getClusterConfig() {
        return config;
    }

}
