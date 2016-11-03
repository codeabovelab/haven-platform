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

import com.codeabovelab.dm.cluman.cluster.docker.model.Network;
import com.codeabovelab.dm.cluman.ds.swarm.NetworkManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/ui/api/networks", produces = APPLICATION_JSON_VALUE)
public class NetworkApi {

    @Autowired
    private NetworkManager networkManager;

    @RequestMapping(value = "{cluster}/all", method = RequestMethod.GET)
    public List<Network> getNetworks(@PathVariable("cluster") String clusterName) {
        return networkManager.getNetworks(clusterName);
    }

    @RequestMapping(value = "{cluster}/create", method = RequestMethod.POST)
    public void createNetworks(@PathVariable("cluster") String clusterName) {
        networkManager.createNetwork(clusterName);
    }

}
