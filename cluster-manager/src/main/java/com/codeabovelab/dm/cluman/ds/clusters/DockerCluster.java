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
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.RemoveNodeArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.SwarmLeaveArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.*;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.*;
import com.codeabovelab.dm.cluman.ds.container.ContainerCreator;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.ds.nodes.NodeRegistration;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.security.TempAuth;
import com.codeabovelab.dm.cluman.utils.AddressUtils;
import com.codeabovelab.dm.common.utils.SingleValueCache;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;

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
                NodeStorage nodeStorage = getNodeStorage();
                service = nodeStorage.getNodeService(name);
                if(service != null) {
                    // in some cases node may has different cluster, it cause undefined behaviour
                    // therefore we must force node to new cluster
                    nodeStorage.setNodeCluster(name, DockerCluster.this.getName());
                }
            }
        }
    }

    @Data
    @lombok.Builder(builderClassName = "Builder")
    private static class ClusterData {
        private final String workerToken;
        private final String managerToken;
        private final List<String> managers;
    }

    /**
     * List of cluster manager nodes.
     */
    private final Map<String, Manager> managers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutor;
    private final SingleValueCache<ClusterData> data = SingleValueCache.builder(() -> {
        DockerService docker = getDockerOrNull();
        if(docker == null) {
            return null;
        }
        SwarmInspectResponse swarm = docker.getSwarm();
        DockerServiceInfo info = docker.getInfo();
        JoinTokens tokens = swarm.getJoinTokens();
        return ClusterData.builder()
          .managerToken(tokens.getManager())
          .workerToken(tokens.getWorker())
          .managers(info.getSwarm().getManagers())
          .build();
    })
      .nullStrategy(SingleValueCache.NullStrategy.DIRTY)
      .timeAfterWrite(Long.MAX_VALUE)// we cache for always, but must invalidate it at cluster reinitialization
      .build();
    private final SingleValueCache<Map<String, SwarmNode>> nodesMap;
    private ContainerStorage containerStorage;
    private ContainerCreator containerCreator;
    private ContainersManager containers;

    DockerCluster(DiscoveryStorageImpl storage, DockerClusterConfig config) {
        super(config, storage, Collections.singleton(Feature.SWARM_MODE));
        long cacheTimeAfterWrite = config.getConfig().getCacheTimeAfterWrite();
        nodesMap = SingleValueCache.builder(() -> {
            return loadNodesMap();
        }).timeAfterWrite(cacheTimeAfterWrite)
          .nullStrategy(SingleValueCache.NullStrategy.DIRTY)
          .build();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat(getClass().getSimpleName() + "-" + getName() + "-%d")
          .build());
    }

    @Autowired
    void setContainerStorage(ContainerStorage containerStorage) {
        this.containerStorage = containerStorage;
    }

    @Autowired
    void setContainerCreator(ContainerCreator containerCreator) {
        this.containerCreator = containerCreator;
    }

    private void onNodeEvent(NodeEvent e) {
        NodeInfo old = e.getOld();
        NodeInfo curr = e.getCurrent();
        final String thisCluster = getName();
        final String nodeName = e.getNode().getName();
        boolean nowOur = curr != null && thisCluster.equals(curr.getCluster());
        boolean wasOur = old != null && thisCluster.equals(old.getCluster());
        if (wasOur != nowOur) {
            nodesMap.invalidate();
            // force update nodes
            scheduledExecutor.execute(this::updateNodes);
        }
    }


    protected void closeImpl() {
        this.scheduledExecutor.shutdownNow();
    }

    protected void initImpl() {
        List<String> hosts = this.config.getManagers();
        Assert.notEmpty(hosts, "Cluster config '" + getName() + "' must contains at least one manager host.");
        hosts.forEach(host -> managers.putIfAbsent(host, new Manager(host)));
        initCluster(hosts.get(0));

        this.containers = new DockerClusterContainers(this, this.containerStorage, this.containerCreator);

        // so docker does not send any events about new coming nodes, and we must refresh list of them
        this.scheduledExecutor.scheduleWithFixedDelay(this::updateNodes, 30L, 30L, TimeUnit.SECONDS);
        getNodeStorage().getNodeEventSubscriptions().subscribe(this::onNodeEvent);
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
    public List<NodeInfo> getNodes() {
        Map<String, SwarmNode> map = nodesMap.get();
        ImmutableList.Builder<NodeInfo> b = ImmutableList.builder();
        map.forEach((k, v) -> {
            NodeInfo ni = getNodeStorage().getNodeInfo(getNodeName(v));
            if(ni != null && Objects.equals(ni.getCluster(), getName())) {
                b.add(ni);
            }
        });
        return b.build();
    }

    private Map<String, SwarmNode> loadNodesMap() {
        DockerService docker = getDockerOrNull();
        if(docker == null) {
            return null;
        }
        List<SwarmNode> nodes = docker.getNodes(null);
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
        return map;
    }

    private void updateNodes() {
        try (TempAuth ta = TempAuth.asSystem()) {
            Map<String, SwarmNode> map = nodesMap.get();
            if(map == null) {
                log.error("Can not load map of cluster nodes.");
                return;
            }
            //check that all nodes marked 'our' is in map
            Collection<NodeInfo> nodes = getNodeStorage().getNodes(this::isFromSameCluster);
            Map<String, SwarmNode> localMap = map;
            nodes.forEach((ni) -> {
                String name = ni.getName();
                SwarmNode sn = localMap.get(name);
                if(sn != null && sn.getStatus().getState() != SwarmNode.NodeState.DOWN) {
                    return;
                }
                // down node may mean that it simply leave from cluster but not removed, we must try to join it
                //
                joinWorker(name);

            });
            // add nodes which is not in cluster
            boolean[] modified = new boolean[]{false};
            map.forEach((name, sn) -> {
                SwarmNode.State status = sn.getStatus();
                String address = getNodeAddress(sn);
                if(StringUtils.isEmpty(address) || status.getState() != SwarmNode.NodeState.DOWN) {
                    return;
                }
                registerNode(name, address);
                modified[0] = true;
            });
            if(modified[0]) {
                // we touch some 'down' nodes and must reload list for new status
                map = loadNodesMap();
            }
            if(map == null) {
                log.error("Can not load map of cluster nodes.");
                return;
            }
            map.forEach((name, sn) -> {
                NodeInfo ni = updateNode(sn);
            });
        } catch (Exception e) {
            log.error("Can not update list of nodes due to error.", e);
        }
    }

    private String getNodeAddress(SwarmNode sn) {
        String address = sn.getStatus().getAddress();
        if(StringUtils.isEmpty(address)) {
            return address;
        }
        if(!AddressUtils.hasPort(address)) {
            address = AddressUtils.setPort(address, 2375);
        }
        return address;
    }

    /**
     * Join node as worker to this docker cluster
     * @param name name of node
     */
    private void joinWorker(String name) {
        //join to swarm
        log.info("Begin join node '{}' to '{}'", name, getName());
        ClusterData clusterData = data.get();
        String workerToken = clusterData.getWorkerToken();
        DockerService ds = getNodeStorage().getNodeService(name);
        if(ds == null) {
            log.warn("Can not join node '{}', it does not have registered docker service", name);
            return;
        }
        SwarmJoinCmd cmd = new SwarmJoinCmd();
        cmd.setToken(workerToken);
        this.managers.forEach((k, v) -> {
            cmd.getManagers().addAll(clusterData.getManagers());
        });
        cmd.setListen(ds.getAddress());
        ServiceCallResult res = ds.joinSwarm(cmd);
        log.info("Result of joining node '{}': {} {}", name, res.getCode(), res.getMessage());
    }

    private void leave(String node, String id) {
        log.info("Begin leave node '{}' from '{}'", node, getName());
        DockerService ds = getNodeStorage().getNodeService(node);
        if(ds == null) {
            log.warn("Can not leave node '{}' from cluster, node does not have registered docker service", node);
            return;
        } else {
            ServiceCallResult res = ds.leaveSwarm(new SwarmLeaveArg());
            log.info("Result of leave node '{}' : {} {}", node, res.getCode(), res.getMessage());
        }
        DockerService docker = getDocker();
        ServiceCallResult rmres = docker.removeNode(new RemoveNodeArg(id).force(true));
        log.info("Result of remove node '{}' from cluster: {} {}", node, rmres.getCode(), rmres.getMessage());
    }


    private String getNodeName(SwarmNode sn) {
        return sn.getDescription().getHostname();
    }

    private NodeInfo updateNode(SwarmNode sn) {
        String nodeName = getNodeName(sn);
        String address = getNodeAddress(sn);
        if(StringUtils.isEmpty(address)) {
            log.warn("Node {} does not has address, it usual for docker prior to 1.13 version.", nodeName);
            return null;
        }

        NodeStorage ns = getNodeStorage();
        NodeRegistration nr = ns.updateNode(nodeName, Integer.MAX_VALUE, b -> {
            String oldCluster = b.getCluster();
            final String cluster = getName();
            if(oldCluster != null && !cluster.equals(oldCluster)) {
                return;
            }
            b.idInCluster(sn.getId());
            b.address(address);
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
        if(!Objects.equals(getName(), nr.getCluster())) {
            log.info("Node {} is from another cluster: '{}', we remove it from our cluster: '{}'.", nodeName, nr.getCluster(), getName());
            leave(nodeName, sn.getId());
            return null;
        }
        return nr.getNodeInfo();
    }

    private void registerNode(String node, String address) {
        try {
            NodeStorage nodes = getDiscoveryStorage().getNodeStorage();
            nodes.registerNode(node, address);
        } catch (Exception e) {
            log.error("While register node '{}' at '{}'", node, address, e);
        }
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
        DockerService ds = getDockerOrNull();
        if(ds == null) {
            throw new IllegalStateException("Cluster " + getName() + " has not any alive manager node.");
        }
        return ds;
    }

    private DockerService getDockerOrNull() {
        for (Manager node : managers.values()) {
            DockerService service = node.getService();
            if (service != null) {
                return service;
            }
        }
        return null;
    }

    private SwarmSpec getSwarmConfig() {
        SwarmSpec sc = new SwarmSpec();
        return sc;
    }

    @Override
    public ContainersManager getContainers() {
        return containers;
    }
}
