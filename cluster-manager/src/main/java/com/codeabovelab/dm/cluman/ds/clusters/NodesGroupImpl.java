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

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetContainersArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.filter.Filter;
import com.codeabovelab.dm.cluman.cluster.filter.FilterFactory;
import com.codeabovelab.dm.cluman.ds.SwarmClusterContainers;
import com.codeabovelab.dm.cluman.ds.container.ContainerCreator;
import com.codeabovelab.dm.cluman.ds.container.ContainerRegistration;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.model.*;
import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import lombok.Singular;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

/**
 * Node group managed 'manually'. It allow to view multiple nodes as single entity.
 */
@ToString(callSuper = true)
class NodesGroupImpl extends AbstractNodesGroup<DefaultNodesGroupConfig> {

    public interface ContainersProvider {
        List<DockerContainer> getContainers(NodesGroupImpl ng, GetContainersArg arg);
    }

    private static class DefaultContainersProvider implements ContainersProvider {

        public List<DockerContainer> getContainers(NodesGroupImpl ng, GetContainersArg arg) {
            List<DockerContainer> list = new ArrayList<>();
            NodeStorage nodeStorage = ng.getNodeStorage();
            for(Node node: ng.getNodes()) {
                DockerService service = nodeStorage.getNodeService(node.getName());
                if(VirtualDockerService.isOffline(service)) {
                    // due to different causes service can be null
                    continue;
                }
                try {
                    List<DockerContainer> nodeContainer = service.getContainers(arg);
                    list.addAll(nodeContainer);
                } catch (AccessDeniedException e) {
                    //nothing
                }
            }
            return list;
        }
    }

    public static class AllContainersProvider implements ContainersProvider {
        @Override
        public List<DockerContainer> getContainers(NodesGroupImpl ng, GetContainersArg arg) {
            List<ContainerRegistration> containers = ng.containerStorage.getContainers();
            ArrayList<DockerContainer> list = new ArrayList<>(containers.size());
            containers.forEach(cr -> {list.add(cr.getContainer());});
            return list;
        }
    }

    private final ContainersProvider DEFAULT_CONTAINERS_PROVIDER = new DefaultContainersProvider();
    private final VirtualDockerService service;
    private ContainersManager containers;
    private Filter predicate;
    private FilterFactory filterFactory;
    private ContainerCreator containerCreator;
    private ContainerStorage containerStorage;
    private ContainersProvider containersProvider;

    @Builder
    public NodesGroupImpl(DiscoveryStorageImpl storage,
                          Filter predicate,
                          DefaultNodesGroupConfig config,
                          @Singular Set<Feature> features) {
        super(config, storage, ImmutableSet.<Feature>builder()
          .addAll(features == null? Collections.emptySet(): features)
          .build());
        this.containersProvider = DEFAULT_CONTAINERS_PROVIDER;
        this.service = new VirtualDockerService(this);

        this.predicate = predicate;
    }

    public void setContainersProvider(ContainersProvider containersProvider) {
        this.containersProvider = containersProvider;
    }

    @Autowired
    void setContainerCreator(ContainerCreator containerCreator) {
        this.containerCreator = containerCreator;
    }

    @Autowired
    void setFilterFactory(FilterFactory filterFactory) {
        this.filterFactory = filterFactory;
    }

    @Autowired
    void setContainerStorage(ContainerStorage containerStorage) {
        this.containerStorage = containerStorage;
    }

    @Override
    protected void initImpl() {
        if(predicate != null) {
            config.setNodeFilter(predicate.getExpression());
        } else {
            this.predicate = filterFactory.createFilter(config.getNodeFilter());
        }
        this.containers = new SwarmClusterContainers(this::getDocker, this.containerCreator);
    }

    @Override
    public DockerService getDocker() {
        return this.service;
    }

    @Override
    public ContainersManager getContainers() {
        return this.containers;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<NodeInfo> getNodes() {
        return getNodesInternal();
    }

    @Override
    public ServiceCallResult updateNode(NodeUpdateArg arg) {
        return ServiceCallResult.unsupported();
    }

    private List<NodeInfo> getNodesInternal() {
        return getNodeStorage().getNodes(predicate);
    }

    @Override
    public Collection<String> getGroups() {
        Set<String> clusters = new HashSet<>();
        getNodesInternal().forEach(n -> {
            clusters.add(n.getCluster());
        });
        return clusters;
    }

    public String getNodeFilter() {
        synchronized (lock) {
            return config.getNodeFilter();
        }
    }

    public void setNodeFilter(String nodeFilter) {
        synchronized (lock) {
            onSet("nodeFilter", this.config.getNodeFilter(), nodeFilter);
            this.config.setNodeFilter(nodeFilter);
            this.predicate = filterFactory.createFilter(nodeFilter);
        }
    }

    @Override
    public boolean hasNode(String id) {
        return getNodeStorage().hasNode(predicate, id);
    }

    ContainerStorage getContainerStorage() {
        return this.containerStorage;
    }

    List<DockerContainer> getContainersImpl(GetContainersArg arg) {
        return this.containersProvider.getContainers(this, arg);
    }
}
