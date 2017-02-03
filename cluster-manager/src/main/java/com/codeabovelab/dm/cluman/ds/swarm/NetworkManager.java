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

package com.codeabovelab.dm.cluman.ds.swarm;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.CreateNetworkCmd;
import com.codeabovelab.dm.cluman.cluster.docker.model.Network;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.cluman.security.TempAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Component
public class NetworkManager implements Consumer<NodeEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkManager.class);
    private static final String OVERLAY_DRIVER = "overlay";

    private final DiscoveryStorage discoveryStorage;

    @Autowired
    public NetworkManager(DiscoveryStorage discoveryStorage, @Qualifier(NodeEvent.BUS) MessageBus<NodeEvent> bus) {
        this.discoveryStorage = discoveryStorage;
        bus.subscribe(this);
    }

    /**
     * Creates overlay network with cluster name <p/>
     * all containers will have container.cluster-name DNS address <p/>
     * Docker daemon should have --cluster-store and --cluster-advertise options. <p/>
     * @param clusterName
     */
    public ServiceCallResult createNetwork(String clusterName) {
        NodesGroup group = discoveryStorage.getCluster(clusterName);
        return createNetwork(group, clusterName);
    }

    public ServiceCallResult createNetwork(NodesGroup group, String networkName) {
        Set<NodesGroup.Feature> features = group.getFeatures();
        if(!features.contains(NodesGroup.Feature.SWARM) && !features.contains(NodesGroup.Feature.SWARM_MODE)) {
            // non swarm groups does not support network creation
            return new ServiceCallResult().code(ResultCode.NOT_MODIFIED).message("not supported for this group type");
        }

        CreateNetworkCmd cmd = new CreateNetworkCmd();
        cmd.setName(networkName);
        cmd.setDriver(OVERLAY_DRIVER);
        cmd.setCheckDuplicate(true);
        return createNetwork(group, cmd);
    }

    public ServiceCallResult createNetwork(NodesGroup group, CreateNetworkCmd cmd) {
        DockerService service = group.getDocker();
        LOG.debug("About to create network '{}' for cluster '{}'", cmd, group.getName());
        ServiceCallResult res = service.createNetwork(cmd);
        if (res.getCode() == ResultCode.ERROR) {
            LOG.error("can't create network for cluster {} due: {}", group.getName(), res.getMessage());
        }
        return res;
    }

    /**
     * List networks
     *
     * @param clusterName
     * @return
     */
    public List<Network> getNetworks(String clusterName) {
        DockerService service = discoveryStorage.getService(clusterName);
        List<Network> networks = service.getNetworks();
        LOG.debug("networks for cluster {}: {}", clusterName, networks);
        return networks;
    }

    @Override
    public void accept(NodeEvent nodeEvent) {
        NodeInfo node = nodeEvent.getCurrent();
        if (node == null) {
            return;
        }
        try(TempAuth ta = TempAuth.asSystem()) {
            NodesGroup cluster = discoveryStorage.getClusterForNode(node.getName());
            if (cluster == null) {
                LOG.warn("Node without cluster {}", node);
                return;
            }
            String clusterName = cluster.getName();
            cluster.init();
            NodeGroupState state = cluster.getState();
            if (!state.isOk()) {
                LOG.warn("Can not create network due cluster '{}' in '{}' state.", clusterName, state.getMessage());
                return;
            }
            List<Network> networks = cluster.getDocker().getNetworks();
            LOG.debug("Networks {}", networks);
            Optional<Network> any = networks.stream().filter(n -> n.getName().equals(clusterName)).findAny();
            if (!any.isPresent()) {
                createNetwork(cluster, clusterName);
            }
        }
    }
}
