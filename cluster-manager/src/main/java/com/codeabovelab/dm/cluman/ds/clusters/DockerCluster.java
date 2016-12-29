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
import com.codeabovelab.dm.cluman.cluster.docker.management.result.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.SwarmConfig;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.SwarmInitCmd;
import com.codeabovelab.dm.cluman.ds.nodes.NodeRegistration;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.utils.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A kind of nodegroup which is managed by 'docker' in 'swarm mode'.
 */
@Slf4j
public class DockerCluster extends AbstractNodesGroup<DockerClusterConfig> {

    private final class SwarmNode {
        private final String name;
        private DockerService service;

        SwarmNode(String name) {
            this.name = name;
        }

        void init() {
            DockerServices dses = getDiscoveryStorage().getDockerServices();
            service = dses.getNodeService(name);
            Assert.notNull(service, "Can not find docker service for '" + name + "' node.");
            SwarmInitCmd cmd = new SwarmInitCmd();
            cmd.setConfig(getSwarmConfig());
            String address = service.getAddress();
            address = AddressUtils.setPort(address, config.getSwarmPort());
            cmd.setListenAddr(address);
            SwarmInitResult res = service.initSwarm(cmd);
            if(res.getCode() == ResultCode.OK) {
                return;
            }
            log.error("Can not initialize swarm-mode cluster on '{}' due to error: {}", name, res.getMessage());
        }

    }

    /**
     * List of cluster master nodes.
     */
    private final List<SwarmNode> swarmNodes = new CopyOnWriteArrayList<>();

    @lombok.Builder(builderClassName = "Builder")
    DockerCluster(DockerClusterConfig config, DiscoveryStorageImpl storage) {
        super(config, storage, Collections.singleton(Feature.SWARM_MODE));
    }

    protected void init() {
        Collection<String> hosts = Collections.singleton(this.config.getConfig().getHost());
        Assert.isTrue(!hosts.isEmpty(), "Cluster config '" + getName() + "' must contains at lest one host.");
        hosts.forEach(host -> swarmNodes.add(new SwarmNode(host)));
        swarmNodes.forEach(SwarmNode::init);
    }

    @Override
    public Collection<NodeInfo> getNodes() {
        return getNodeStorage().getNodes(this::isFromSameCluster);
    }

    private boolean isFromSameCluster(NodeRegistration nr) {
        return nr != null && getName().equals(nr.getNodeInfo().getCluster());
    }

    @Override
    public Collection<String> getGroups() {
        return Collections.emptySet();
    }

    @Override
    public boolean hasNode(String id) {
        NodeRegistration nr = getNodeStorage().getNodeRegistration(id);
        return isFromSameCluster(nr);
    }

    @Override
    public DockerService getDocker() {
        SwarmNode swarmNode = swarmNodes.get(0);
        //TODO we must check that node is online, also return leader is better
        return swarmNode.service;
    }

    private SwarmConfig getSwarmConfig() {
        SwarmConfig sc = new SwarmConfig();
        return sc;
    }
}
