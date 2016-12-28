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
import com.codeabovelab.dm.cluman.model.NodeInfo;

import java.util.Collection;

/**
 * A kind of nodegroup which is managed by 'docker' in 'swarm mode'.
 */
public class DockerCluster extends AbstractNodesGroup<DockerClusterConfig> {

    @lombok.Builder(builderClassName = "Builder")
    DockerCluster(DockerClusterConfig config, DiscoveryStorageImpl storage, Collection<Feature> features) {
        super(config, storage, features);
    }

    @Override
    public Collection<NodeInfo> getNodes() {
        return null;
    }

    @Override
    public Collection<String> getGroups() {
        return null;
    }

    @Override
    public boolean hasNode(String id) {
        return false;
    }

    @Override
    public DockerService getDocker() {
        return null;
    }
}
