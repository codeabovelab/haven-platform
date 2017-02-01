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
import com.codeabovelab.dm.cluman.cluster.docker.model.Network;
import com.codeabovelab.dm.cluman.ds.swarm.NetworkManager;
import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.cluman.ui.model.UIResult;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/ui/api/networks", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NetworkApi {

    private final NetworkManager networkManager;
    private final DiscoveryStorage discoveryStorage;

    @RequestMapping(value = {"{cluster}/"}, method = RequestMethod.GET)
    public List<Network> getNetworks(@PathVariable("cluster") String clusterName) {
        return networkManager.getNetworks(clusterName);
    }

    @RequestMapping(value = "{cluster}/{network}", method = RequestMethod.POST)
    public ResponseEntity<UIResult> createNetwork(@PathVariable("cluster") String clusterName, @PathVariable("network") String network) {
        NodesGroup group = discoveryStorage.getCluster(clusterName);
        ExtendedAssert.notFound(group, "Cluster " + clusterName + " not found");
        ServiceCallResult res = networkManager.createNetwork(group, network);
        return UiUtils.createResponse(res);
    }

    @RequestMapping(value = "{cluster}/{network}", method = RequestMethod.GET)
    public Object getNetwork(@PathVariable("cluster") String clusterName, @PathVariable("network") String netId) {
        NodesGroup group = discoveryStorage.getCluster(clusterName);
        ExtendedAssert.notFound(group, "Cluster " + clusterName + " not found");
        Network net = group.getDocker().getNetwork(netId);
        //TODO return UI network
        return net;
    }

    @RequestMapping(value = "{cluster}/{network}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteNetwork(@PathVariable("cluster") String clusterName, @PathVariable("network") String network) {
        NodesGroup group = discoveryStorage.getCluster(clusterName);
        ExtendedAssert.notFound(group, "Cluster " + clusterName + " not found");
        ServiceCallResult res = group.getDocker().deleteNetwork(network);
        return UiUtils.createResponse(res);
    }
}
