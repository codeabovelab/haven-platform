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
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.SwarmNode;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.SwarmSpec;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.SwarmInitCmd;
import com.codeabovelab.dm.cluman.ds.nodes.NodeRegistration;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.security.TempAuth;
import com.codeabovelab.dm.cluman.utils.AddressUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A kind of nodegroup which is managed by 'docker' in 'swarm mode'.
 */
@Slf4j
public class DockerCluster extends AbstractNodesGroup<DockerClusterConfig> {

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
    private final ScheduledExecutorService scheduledExecutor;

    @lombok.Builder(builderClassName = "Builder")
    DockerCluster(DockerClusterConfig config, DiscoveryStorageImpl storage) {
        super(config, storage, Collections.singleton(Feature.SWARM_MODE));
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat(getClass().getSimpleName() + "-" + getName() + "-%d")
          .build());
        // so docker does not send any events about new coming nodes, and we must refresh list of them
        this.scheduledExecutor.scheduleWithFixedDelay(this::updateNodes, 30L, 30L, TimeUnit.SECONDS);
    }

    protected void closeImpl() {
        this.scheduledExecutor.shutdownNow();
    }

    protected void initImpl() {
        List<String> hosts = this.config.getManagers();
        Assert.notEmpty(hosts, "Cluster config '" + getName() + "' must contains at least one manager host.");
        hosts.forEach(host -> managers.putIfAbsent(host, new Manager(host)));
        initCluster(hosts.get(0));
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
        } else if(selectedManager == null) {
            throw new IllegalStateException("We has cluster: " + clusters + " but no one managers.");
        }
        //and then we must join all managers to created cluster
        for(Manager node: managers.values()) {
            if(node == selectedManager) {
                continue;
            }
            //TODO node.service.joinSwarm();
        }
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
        List<SwarmNode> nodes = getDocker().getNodes(null);
        // docker may produce node duplicated
        // see https://github.com/docker/docker/issues/24088
        // therefore we must fina one actual node in duplicates
        Map<String, SwarmNode> map = new HashMap<>();
        nodes.forEach(sn -> {
            String nodeName = getNodeName(sn);
            map.compute(nodeName, (key, old) -> {
                // use new node if old null or down
                if(old == null || old.getStatus().getState() == SwarmNode.NodeState.DOWN) {
                    old = sn;
                }
                return old;
            });
        });
        ImmutableList.Builder<NodeInfo> b = ImmutableList.builder();
        map.forEach((k, v) -> {
            NodeInfo ni = updateNode(v);
            if(ni != null) {
                b.add(ni);
            }
        });
        return b.build();
    }

    private void updateNodes() {
        try (TempAuth ta = TempAuth.asSystem()) {
            getNodes();
        } catch (Exception e) {
            log.error("Can not update list of nodes due to error.", e);
        }
    }

    private String getNodeName(SwarmNode sn) {
        return sn.getDescription().getHostname();
    }

    private NodeInfo updateNode(SwarmNode sn) {
        String nodeName = getNodeName(sn);
        String address = sn.getStatus().getAddress();
        if(!StringUtils.hasText(address)) {
            log.warn("Node {} does not has address, it usual for docker prior to 1.13 version.", nodeName);
            return null;
        }
        NodeStorage ns = getNodeStorage();
        NodeRegistration nr = ns.updateNode(nodeName, Integer.MAX_VALUE, b -> {
            b.address(address);
            b.cluster(getName());
            NodeMetrics.Builder nmb = NodeMetrics.builder();
            NodeMetrics.State state = getState(sn);
            nmb.state(state);
            nmb.healthy(state == NodeMetrics.State.HEALTHY);
            b.mergeHealth(nmb.build());
            Map<String, String> labels = sn.getDescription().getEngine().getLabels();
            if(labels != null) {
                b.labels(labels);
            }
        });
        return nr.getNodeInfo();
    }

    private NodeMetrics.State getState(SwarmNode sn) {
        SwarmNode.NodeAvailability availability = sn.getSpec().getAvailability();
        SwarmNode.NodeState state = sn.getStatus().getState();
        if (state == SwarmNode.NodeState.READY && availability == SwarmNode.NodeAvailability.ACTIVE) {
            return NodeMetrics.State.HEALTHY;
        }
        if (state == SwarmNode.NodeState.DOWN ||
          availability == SwarmNode.NodeAvailability.DRAIN ||
          availability == SwarmNode.NodeAvailability.PAUSE) {
            return NodeMetrics.State.MAINTENANCE;
        }
        return NodeMetrics.State.DISCONNECTED;
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
