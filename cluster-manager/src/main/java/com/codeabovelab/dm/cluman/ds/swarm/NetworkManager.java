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
import com.codeabovelab.dm.cluman.cluster.docker.model.CreateNetworkCmd;
import com.codeabovelab.dm.cluman.cluster.docker.model.CreateNetworkResponse;
import com.codeabovelab.dm.cluman.cluster.docker.model.Network;
import com.codeabovelab.dm.cluman.ds.clusters.AbstractNodesGroup;
import com.codeabovelab.dm.cluman.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class NetworkManager {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkManager.class);
    private static final String OVERLAY_DRIVER = "overlay";
    private final AbstractNodesGroup<?> group;

    public NetworkManager(AbstractNodesGroup<?> group) {
        this.group = group;
    }

    public CreateNetworkResponse createNetwork(String networkName) {
        // remove below code in future
        Set<NodesGroup.Feature> features = group.getFeatures();
        if(!features.contains(NodesGroup.Feature.SWARM) && !features.contains(NodesGroup.Feature.SWARM_MODE)) {
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
        return createNetwork(cmd);
    }

    public CreateNetworkResponse createNetwork(CreateNetworkCmd cmd) {
        DockerService service = group.getDocker();
        LOG.debug("About to create network '{}' for cluster '{}'", cmd, group.getName());
        CreateNetworkResponse res = service.createNetwork(cmd);
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
        DockerService service = group.getDocker();
        List<Network> networks = service.getNetworks();
        LOG.debug("networks for cluster {}: {}", clusterName, networks);
        return networks;
    }
}
