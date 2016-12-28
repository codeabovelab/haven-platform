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
import com.codeabovelab.dm.cluman.cluster.filter.Filter;
import com.codeabovelab.dm.cluman.cluster.filter.FilterFactory;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.model.Node;
import com.codeabovelab.dm.cluman.model.NodeInfo;
import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import lombok.Singular;
import lombok.ToString;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Node group managed 'manually'. It allow to view multiple nodes as single entity.
 */
@ToString(callSuper = true)
class NodesGroupImpl extends AbstractNodesGroup<DefaultNodesGroupConfig> {

    private final DockerServices dockerServices;
    private final VirtualDockerService service;
    private Filter predicate;
    private final FilterFactory filterFactory;

    @Builder
    public NodesGroupImpl(DiscoveryStorageImpl storage,
                          FilterFactory filterFactory,
                          Filter predicate,
                          DefaultNodesGroupConfig config,
                          DockerServices dockerServices,
                          @Singular Set<Feature> features) {
        super(config, storage, ImmutableSet.<Feature>builder()
          .addAll(features == null? Collections.emptySet(): features)
          .build());
        this.dockerServices = dockerServices;
        Assert.notNull(this.dockerServices);
        this.service = new VirtualDockerService(this);
        this.filterFactory = filterFactory;
        this.predicate = predicate;
        if(predicate != null) {
            config.setNodeFilter(predicate.getExpression());
        } else {
            this.predicate = filterFactory.createFilter(config.getNodeFilter());
        }
    }

    DockerServices getDockerServices() {
        return dockerServices;
    }

    @Override
    public DockerService getDocker() {
        return this.service;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<NodeInfo> getNodes() {
        return getNodesInternal();
    }

    private Collection<NodeInfo> getNodesInternal() {
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
}
