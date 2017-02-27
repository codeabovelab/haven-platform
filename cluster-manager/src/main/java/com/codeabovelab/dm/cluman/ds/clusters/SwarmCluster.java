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
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.ds.SwarmClusterContainers;
import com.codeabovelab.dm.cluman.ds.container.ContainerCreator;
import com.codeabovelab.dm.cluman.ds.nodes.NodeRegistration;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.common.kv.WriteOptions;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * A kind of nodegroup which is managed by 'swarm'
 */
@Slf4j
@ToString(callSuper = true)
public final class SwarmCluster extends AbstractNodesGroup<SwarmNodesGroupConfig> {

    private KvMapperFactory kvmf;
    private DockerService docker;
    private ContainersManager containers;
    private ContainerCreator containerCreator;

    SwarmCluster(DiscoveryStorageImpl storage, SwarmNodesGroupConfig config) {
        super(config, storage, Collections.singleton(Feature.SWARM));
    }

    @Autowired
    void setKvmf(KvMapperFactory kvmf) {
        this.kvmf = kvmf;
    }

    @Autowired
    void setContainerCreator(ContainerCreator containerCreator) {
        this.containerCreator = containerCreator;
    }

    private void onNodeEvent(NodeEvent event) {
        NodeEvent.Action action = event.getAction();
        if(NodeEvent.Action.OFFLINE == action) {
            return;
        }
        NodeInfo ni = event.getCurrent();
        boolean delete = false;
        if(ni == null) {
            delete = true;
            ni = event.getOld();
        }
        String nodeName = ni.getName();
        String cluster = ni.getCluster();
        if (!StringUtils.hasText(cluster) || !getName().equals(cluster)) {
            return;
        }
        Assert.doesNotContain(cluster, "/", "Bad cluster name: " + cluster);
        String address = ni.getAddress();
        if(delete) {
            try {
                kvmf.getStorage().delete(getDiscoveryKey(cluster, address), null);
            } catch (Exception e) {
                log.error("Can not delete swarm registration: of node {} from cluster {}", address, cluster, e);
            }
            return;
        }
        NodeRegistration nr = getNodeStorage().getNodeRegistration(nodeName);
        int ttl = nr.getTtl();
        if (ttl < 1) {
            return;
        }
        // we update node record for swarm discovery in KV for each event
        try {
            kvmf.getStorage().set(getDiscoveryKey(cluster, address),
              address,
              WriteOptions.builder().ttl(ttl).build());
        } catch (Exception e) {
            log.error("Can not update swarm registration: of node {} from cluster {}", address, cluster, e);
        }
        createDefaultNetwork();
    }

    private String getDiscoveryKey(String cluster, String address) {
        return "/discovery/" + cluster + "/docker/swarm/nodes/" + address;
    }

    @Override
    public boolean hasNode(String id) {
        NodeRegistration nr = getNodeStorage().getNodeRegistration(id);
        return isFromSameCluster(nr);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<NodeInfo> getNodes() {
        return getNodesInfo();
    }

    @Override
    public ServiceCallResult updateNode(NodeUpdateArg arg) {
        return ServiceCallResult.unsupported();
    }

    @Override
    public Collection<String> getGroups() {
        return Collections.emptySet();
    }

    private List<NodeInfo> getNodesInfo() {
        return getNodeStorage().getNodes(this::isFromSameCluster);
    }

    private boolean isFromSameCluster(NodeRegistration nr) {
        return nr != null && getName().equals(nr.getNodeInfo().getCluster());
    }

    @Override
    public DockerService getDocker() {
        return docker;
    }

    @Override
    protected void initImpl() {
        getNodeStorage().getNodeEventSubscriptions().subscribe(this::onNodeEvent);
        this.containers = new SwarmClusterContainers(this::getDocker, this.containerCreator);

        DockerServices dses = this.getDiscoveryStorage().getDockerServices();
        this.docker = dses.getOrCreateCluster(getClusterConfig(), (dsb) -> {
            dsb.setInfoInterceptor(this::dockerInfoModifier);
        });
    }

    private void dockerInfoModifier(DockerServiceInfo.Builder dib) {
        // swarm use name of host which run cluster, we must use cluster name for prevent user confusing
        dib.setName(getName());
        Map<String, NodeInfo> map = new HashMap<>();
        int offNodes = 0;
        for (NodeInfo nodeInfo : dib.getNodeList()) {
            map.put(nodeInfo.getName(), nodeInfo);
            if (!nodeInfo.isOn()) {
                offNodes++;
            }
        }

        for (NodeInfo nodeInfo : getNodesInfo()) {
            if (!map.containsKey(nodeInfo.getName())) {
                // if node is on but not inside cluster yet -> set status pending
                if (nodeInfo.isOn()) {
                    nodeInfo = NodeInfoImpl.builder()
                            .from(nodeInfo)
                            .health(NodeMetrics.builder()
                                    .from(nodeInfo.getHealth())
                                    .state(NodeMetrics.State.PENDING).build()).build();
                }
                map.put(nodeInfo.getName(), nodeInfo);
                offNodes++;
            }
        }
        dib.setNodeList(map.values());
        dib.getNodeList().sort(null);
        dib.setOffNodeCount(offNodes);
        dib.setNodeCount(dib.getNodeList().size() - offNodes);
    }

    @Override
    public ContainersManager getContainers() {
        return containers;
    }

    public void setClusterConfig(ClusterConfig cc) {
        synchronized (lock) {
            ClusterConfigImpl newConf = fixConfig(cc);
            onSet("config", this.config.getConfig(), newConf);
            this.config.setConfig(newConf);
        }
    }

    public ClusterConfigImpl getClusterConfig() {
        synchronized (lock) {
            ClusterConfigImpl config = this.config.getConfig();
            return config;
        }
    }

    /**
     * Sometime external config mey be partially filled, or has wrong value. We can not reject this,
     * for legacy reasons, therefore need to fix it manually.
     * @param cc source
     * @return fixed copy of config
     */
    private ClusterConfigImpl fixConfig(ClusterConfig cc) {
        ClusterConfigImpl newConf;
        if(getName().equals(cc.getCluster())) {
            newConf = ClusterConfigImpl.of(cc);
        } else {
            newConf = ClusterConfigImpl.builder(cc).cluster(getName()).build();
        }
        return newConf;
    }

    @Override
    protected void onConfig() {
        ClusterConfigImpl old = this.config.getConfig();
        ClusterConfigImpl fixed = fixConfig(old);
        this.config.setConfig(fixed);
    }
}
