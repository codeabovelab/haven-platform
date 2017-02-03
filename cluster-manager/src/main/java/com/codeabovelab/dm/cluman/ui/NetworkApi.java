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

import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.CreateNetworkCmd;
import com.codeabovelab.dm.cluman.cluster.docker.model.CreateNetworkResponse;
import com.codeabovelab.dm.cluman.cluster.docker.model.Network;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.ds.swarm.NetworkManager;
import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.cluman.ui.model.*;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * <b>Note: network name may contains '/' and other symbols which is illegal in url path</b>, therefore we use
 * 'RequestParam' instead 'PathVariable'.
 */
@RestController
@RequestMapping(value = "/ui/api/networks", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NetworkApi {

    private final NetworkManager networkManager;
    private final DiscoveryStorage discoveryStorage;
    private final ContainerStorage containerStorage;

    @RequestMapping(path = "list", method = RequestMethod.GET)
    public List<UiNetwork> getNetworks(@RequestParam("cluster") String clusterName) {
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

    @RequestMapping(path = "create", method = RequestMethod.POST)
    public ResponseEntity<?> createNetwork(@RequestParam("cluster") String clusterName,
                                                  @RequestParam("network") String network,
                                                  @RequestBody UiNetworkBase body) {
        NodesGroup group = discoveryStorage.getCluster(clusterName);
        ExtendedAssert.notFound(group, "Cluster '" + clusterName + "' not found");
        CreateNetworkCmd cmd = new CreateNetworkCmd();
        if(body != null) {
            body.to(cmd);
        }
        cmd.setName(network);
        cmd.setCheckDuplicate(true);
        CreateNetworkResponse res = networkManager.createNetwork(group, cmd);
        if(res.getCode() == ResultCode.OK) {
            UiNetworkCreateResult uincr = new UiNetworkCreateResult();
            uincr.setId(res.getId());
            String warning = res.getWarning();
            if(StringUtils.hasText(warning)) {
                uincr.setWarning(warning);
            }
            uincr.setName(network);
            return new ResponseEntity<Object>(uincr, HttpStatus.OK);
        }
        return UiUtils.createResponse(res);
    }

    @RequestMapping(path = "get", method = RequestMethod.GET)
    public UiNetworkDetails getNetwork(@RequestParam("cluster") String clusterName,
                                       @RequestParam("network")  String netId) {
        NodesGroup group = discoveryStorage.getCluster(clusterName);
        ExtendedAssert.notFound(group, "Cluster '" + clusterName + "' not found");
        Network net = group.getDocker().getNetwork(netId);
        ExtendedAssert.notFound(net, "Can not found network '" + netId + "' in cluster '" + clusterName + "'");
        UiNetworkDetails uinet = new UiNetworkDetails();
        uinet.setCluster(clusterName);
        return uinet.from(net, containerStorage);
    }

    @RequestMapping(path = "delete", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteNetwork(@RequestParam("cluster") String clusterName,
                                           @RequestParam("network") String network) {
        NodesGroup group = discoveryStorage.getCluster(clusterName);
        ExtendedAssert.notFound(group, "Cluster '" + clusterName + "' not found");
        ServiceCallResult res = group.getDocker().deleteNetwork(network);
        return UiUtils.createResponse(res);
    }
}
