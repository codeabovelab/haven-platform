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
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.PruneNetworksArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.*;
import com.codeabovelab.dm.cluman.ds.clusters.AbstractNodesGroup;
import com.codeabovelab.dm.cluman.ds.clusters.ClusterUtils;
import com.codeabovelab.dm.common.utils.SingleValueCache;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NetworkManager {

    public static final String OVERLAY_DRIVER = "overlay";
    private final SingleValueCache<Map<String, Network>> networksCache;
    private final AbstractNodesGroup<?> group;

    public NetworkManager(AbstractNodesGroup<?> group) {
        this.group = group;
        this.networksCache  = SingleValueCache.builder(this::loadNetworks)
          .timeAfterWrite(TimeUnit.MINUTES, 5L)
          .build();
    }

    private Map<String, Network> loadNetworks() {
        DockerService service = group.getDocker();
        List<Network> networks = service.getNetworks();
        log.debug("Load networks for cluster {}: {}", group.getName(), networks);
        Map<String, Network> map = new HashMap<>();
        if(networks != null) {
            networks.forEach(network -> map.put(network.getId(), network));
        }
        return Collections.unmodifiableMap(map);
    }

    public CreateNetworkResponse createNetwork(String networkName) {
        // remove below code in future
        if(!ClusterUtils.isDockerBased(group)) {
            // non swarm groups does not support network creation
            CreateNetworkResponse res = new CreateNetworkResponse();
            res.code(ResultCode.NOT_MODIFIED).message("not supported for this group type");
            return res;
        }

        CreateNetworkCmd cmd = new CreateNetworkCmd();
        cmd.setName(networkName);
        cmd.setDriver(OVERLAY_DRIVER);
        cmd.setCheckDuplicate(true);
        cmd.setAttachable(true);
        cmd.setIpam(Network.Ipam.DEF_IPAM);
        return createNetwork(cmd);
    }

    public CreateNetworkResponse createNetwork(CreateNetworkCmd cmd) {
        DockerService service = group.getDocker();
        this.networksCache.invalidate();
        log.debug("About to create network '{}' for cluster '{}'", cmd, group.getName());
        CreateNetworkResponse res = service.createNetwork(cmd);
        if (res.getCode() == ResultCode.ERROR) {
            log.error("can't create network for cluster {} due: {}", group.getName(), res.getMessage());
        }
        return res;
    }

    /**
     * List networks
     * @return unmodifiable map of group networks
     */
    public Map<String, Network> getNetworks() {
        Map<String, Network> map = this.networksCache.get();
        return map == null? Collections.emptyMap() : map;
    }

    public ServiceCallResult deleteNetwork(String network) {
        DockerService service = group.getDocker();
        networksCache.invalidate();
        return service.deleteNetwork(network);
    }

    public ServiceCallResult connectNetwork(ConnectNetworkCmd cmd) {
        DockerService service = group.getDocker();
        networksCache.invalidate();
        return service.connectNetwork(cmd);
    }

    public ServiceCallResult disconnectNetwork(DisconnectNetworkCmd cmd) {
        DockerService service = group.getDocker();
        networksCache.invalidate();
        return service.disconnectNetwork(cmd);
    }

    public ServiceCallResult deleteUnusedNetworks() {
        DockerService service = group.getDocker();
        networksCache.invalidate();
        return service.pruneNetworks(new PruneNetworksArg());
    }
}
