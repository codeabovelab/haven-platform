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

package com.codeabovelab.dm.cluman.ui;

import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.CreateNetworkCmd;
import com.codeabovelab.dm.cluman.cluster.docker.model.Network;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.ds.swarm.NetworkManager;
import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.cluman.ui.model.UIResult;
import com.codeabovelab.dm.cluman.ui.model.UiNetwork;
import com.codeabovelab.dm.cluman.ui.model.UiNetworkDetails;
import com.codeabovelab.dm.cluman.ui.model.UiNetworkBase;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/ui/api/networks", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NetworkApi {

    private final NetworkManager networkManager;
    private final DiscoveryStorage discoveryStorage;
    private final ContainerStorage containerStorage;

    @RequestMapping(value = {"{cluster}"}, method = RequestMethod.GET)
    public List<UiNetwork> getNetworks(@PathVariable("cluster") String clusterName) {
        List<Network> networks = networkManager.getNetworks(clusterName);
        ArrayList<UiNetwork> results = new ArrayList<>(networks.size());
        networks.forEach(src -> {
            UiNetwork res = new UiNetwork();
            res.from(src, containerStorage);
            res.setCluster(clusterName);
            results.add(res);
        });
        return results;
    }

    @RequestMapping(value = "{cluster}/{network}", method = RequestMethod.POST)
    public ResponseEntity<UIResult> createNetwork(@PathVariable("cluster") String clusterName,
                                                  @PathVariable("network") String network,
                                                  @RequestBody UiNetworkBase body) {
        NodesGroup group = discoveryStorage.getCluster(clusterName);
        ExtendedAssert.notFound(group, "Cluster " + clusterName + " not found");
        CreateNetworkCmd cmd = new CreateNetworkCmd();
        if(body != null) {
            body.to(cmd);
        }
        cmd.setName(network);
        ServiceCallResult res = networkManager.createNetwork(group, cmd);
        return UiUtils.createResponse(res);
    }

    @RequestMapping(value = "{cluster}/{network}", method = RequestMethod.GET)
    public UiNetworkDetails getNetwork(@PathVariable("cluster") String clusterName, @PathVariable("network") String netId) {
        NodesGroup group = discoveryStorage.getCluster(clusterName);
        ExtendedAssert.notFound(group, "Cluster " + clusterName + " not found");
        Network net = group.getDocker().getNetwork(netId);
        ExtendedAssert.notFound(net, "Can not found network " + clusterName + "/" + netId);
        return new UiNetworkDetails().from(net, containerStorage);
    }

    @RequestMapping(value = "{cluster}/{network}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteNetwork(@PathVariable("cluster") String clusterName, @PathVariable("network") String network) {
        NodesGroup group = discoveryStorage.getCluster(clusterName);
        ExtendedAssert.notFound(group, "Cluster " + clusterName + " not found");
        ServiceCallResult res = group.getDocker().deleteNetwork(network);
        return UiUtils.createResponse(res);
    }
}
