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


import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetContainersArg;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import com.codeabovelab.dm.cluman.model.DockerServiceInfo;
import com.codeabovelab.dm.cluman.model.NodeInfo;
import com.codeabovelab.dm.cluman.ui.model.UISearchQuery;
import com.codeabovelab.dm.cluman.ui.model.UiContainer;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 */
@RestController
@RequestMapping(value = "/ui/api/nodes", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NodesApi {

    private final NodeStorage nodeStorage;
    private final DockerServices dockerServices;
    private final DiscoveryStorage discoveryStorage;
    private final FilterApi filterApi;
    private final ContainerStorage containerStorage;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public Collection<NodeInfo> listNodes() {
        Collection<NodeInfo> nodes = nodeStorage.getNodes((ni) -> true);
        return nodes;
    }

    @RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
    public void deleteNode(@PathVariable("name") String name) {
        nodeStorage.removeNode(name);
    }

    @RequestMapping(value = "/{name}", method = RequestMethod.GET)
    public NodeInfo getNode(@PathVariable("name") String name) {
        return nodeStorage.getNodeInfo(name);
    }

    @RequestMapping(value = "/{name}/containers", method = RequestMethod.GET)
    public List<UiContainer> getContainers(@PathVariable("name") String name) {
        DockerService ds = dockerServices.getNodeService(name);
        ExtendedAssert.notFound(ds, "Can not find docker service for node: " + name);
        List<DockerContainer> containers = ds.getContainers(new GetContainersArg(true));
        List<UiContainer> uics = containers.stream().map(UiContainer::from).collect(Collectors.toList());
        uics.forEach(c -> c.enrich(discoveryStorage, containerStorage));
        return uics;
    }

    @RequestMapping(value = "/filtered", method = RequestMethod.PUT)
    public Collection<NodeInfo> listNodes(@RequestBody UISearchQuery searchQuery) {
        Collection<NodeInfo> nodes = listNodes();
        Collection<NodeInfo> nodeInfos = filterApi.listNodes(nodes, searchQuery);
        return nodeInfos;
    }


}
