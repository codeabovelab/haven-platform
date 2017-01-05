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
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.SwarmSpec;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.SwarmInitCmd;
import com.codeabovelab.dm.cluman.ds.nodes.NodeRegistration;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.utils.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A kind of nodegroup which is managed by 'docker' in 'swarm mode'.
 */
@Slf4j
public class DockerCluster extends AbstractNodesGroup<DockerClusterConfig> {

    private static final int S_BEGIN = 0;
    private static final int S_INITING = 1;
    private static final int S_INITED = 2;
    private static final int S_FAILED = 99;

    private final class Manager {
        private final String name;
        private DockerService service;

        Manager(String name) {
            this.name = name;
        }

        synchronized DockerService getService() {
            loadService();
            return service;
        }

        private synchronized void loadService() {
            if(service == null) {
                DiscoveryStorageImpl ds = getDiscoveryStorage();
                DockerServices dses = ds.getDockerServices();
                service = dses.getNodeService(name);
                if(service != null) {
                    // in some cases node may has different cluster, it cause undefined behaviour
                    // therefore we must force node to new cluster
                    ds.getNodeStorage().setNodeCluster(name, DockerCluster.this.getName());
                }
            }
        }
    }

    /**
     * List of cluster manager nodes.
     */
    private final Map<String, Manager> managers = new ConcurrentHashMap<>();
    private final AtomicInteger state = new AtomicInteger(S_BEGIN);

    @lombok.Builder(builderClassName = "Builder")
    DockerCluster(DockerClusterConfig config, DiscoveryStorageImpl storage) {
        super(config, storage, Collections.singleton(Feature.SWARM_MODE));
    }

    protected void init() {
        if(!state.compareAndSet(S_BEGIN, S_INITING)) {
            return;
        }
        try {
            List<String> hosts = this.config.getManagers();
            Assert.notEmpty(hosts, "Cluster config '" + getName() + "' must contains at least one manager host.");
            hosts.forEach(host -> {
                Manager old = managers.putIfAbsent(host, new Manager(host));
                if(old != null) {
                    throw new IllegalStateException("Cluster contains multiple hosts with same name: " + host);
                }
            });
            initCluster(hosts.get(0));
        } finally {
            state.compareAndSet(S_INITING, S_FAILED);
        }
    }

    private void initCluster(String leaderName) {
        //first we must find if one of nodes has exists cluster
        Map<String, List<String>> clusters = new HashMap<>();
        Manager selectedManager = null;
        int onlineManagers = 0;
        for(Manager node: managers.values()) {
            DockerService service = node.getService();
            if(service == null) {
                continue;
            }
            onlineManagers++;
            DockerServiceInfo info = service.getInfo();
            SwarmInfo swarm = info.getSwarm();
            if(swarm != null) {
                clusters.computeIfAbsent(swarm.getClusterId(), (k) -> new ArrayList<>()).add(node.name);
                if(swarm.isManager()) {
                    selectedManager = node;
                }
            }
        }
        if(onlineManagers == 0) {
            log.warn("cluster '{}' is not inited because no online masters", getName());
            state.compareAndSet(S_INITING, S_BEGIN);
            return;
        }
        if(clusters.size() > 1) {
            throw new IllegalStateException("Managers nodes already united into different cluster: " + clusters);
        }
        if(clusters.isEmpty()) {
            //we must create cluster if no one found
            selectedManager = createCluster(leaderName);
        }
        //and the we must join all managers to created cluster
        for(Manager node: managers.values()) {
            if(node == selectedManager) {
                continue;
            }
            //TODO node.service.joinSwarm();
        }
        state.compareAndSet(S_INITING, S_INITED);
    }

    private Manager createCluster(String leader) {
        Manager manager = managers.get(leader);
        log.info("Begin initialize swarm-mode cluster on '{}'", manager.name);
        SwarmInitCmd cmd = new SwarmInitCmd();
        cmd.setSpec(getSwarmConfig());
        DockerService service = manager.getService();
        String address = service.getAddress();
        address = AddressUtils.setPort(address, config.getSwarmPort());
        cmd.setListenAddr(address);
        SwarmInitResult res = service.initSwarm(cmd);
        if(res.getCode() != ResultCode.OK) {
            throw new IllegalStateException("Can not initialize swarm-mode cluster on '" + manager.name + "' due to error: " + res.getMessage());
        }
        log.info("Initialize swarm-mode cluster on '{}' at address {}", manager.name, address);
        return manager;
    }

    @Override
    public Collection<NodeInfo> getNodes() {
        return getNodeStorage().getNodes(this::isFromSameCluster);
    }

    private boolean isFromSameCluster(NodeRegistration nr) {
        if (nr == null) {
            return false;
        }
        return this.managers.containsKey(nr.getNodeInfo().getName()) ||
            getName().equals(nr.getCluster());
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
        for (Manager node : managers.values()) {
            DockerService service = node.getService();
            if (service != null) {
                return service;
            }
        }
        throw new IllegalStateException("Cluster " + getName() + " has not any alive manager node.");
    }

    private SwarmSpec getSwarmConfig() {
        SwarmSpec sc = new SwarmSpec();
        return sc;
    }
}
