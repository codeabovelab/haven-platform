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
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.ui.model.UISearchQuery;
import com.codeabovelab.dm.cluman.ui.model.UiContainer;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 */
@RestController
@RequestMapping(value = "/ui/api/nodes", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NodesApi {

    private final NodeStorage nodeStorage;
    private final DiscoveryStorage discoveryStorage;
    private final FilterApi filterApi;
    private final ContainerStorage containerStorage;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public Collection<NodeInfo> listNodes() {
        List<NodeInfo> nodes = new ArrayList<>(nodeStorage.getNodes((ni) -> true));
        nodes.replaceAll(this::prepareForUi);
        return nodes;
    }

    private NodeInfo prepareForUi(NodeInfo ni) {
        if(ni == null) {
            return null;
        }
        NodesGroup ng = discoveryStorage.getClusterForNode(ni.getName());
        String clusterName = ng == null? null : ng.getName();
        if(!Objects.equals(clusterName, ni.getCluster())) {
            // currently we have issue: when cluster was deleted we can not add node to another cluster
            // but also we can not remove cluster name from node, because absent of cluster
            // not mean that it will not appeared in future (due to lazy initialisation)
            ni = NodeInfoImpl.builder(ni).cluster(clusterName).build();
        }
        return ni;
    }

    @RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
    public void deleteNode(@PathVariable("name") String name) {
        nodeStorage.removeNode(name);
    }

    @RequestMapping(value = "/{name}", method = RequestMethod.PUT)
    public void addNode(@PathVariable("name") String name, @RequestParam("address") String address) {
        nodeStorage.registerNode(name, address);
    }

    @RequestMapping(value = "/{name}", method = RequestMethod.GET)
    public NodeInfo getNode(@PathVariable("name") String name) {
        return prepareForUi(nodeStorage.getNodeInfo(name));
    }

    @RequestMapping(value = "/{name}/containers", method = RequestMethod.GET)
    public List<UiContainer> getContainers(@PathVariable("name") String name) {
        DockerService ds = nodeStorage.getNodeService(name);
        ExtendedAssert.notFound(ds, "Can not find docker service for node: " + name);
        List<DockerContainer> containers = ds.getContainers(new GetContainersArg(true));
        List<UiContainer> uics = containers.stream().map(UiContainer::from).collect(Collectors.toList());
        uics.forEach(c -> c.enrich(discoveryStorage, containerStorage));
        return uics;
    }

    @RequestMapping(value = "/filtered", method = RequestMethod.PUT)
    public Collection<NodeInfo> listNodes(@RequestBody UISearchQuery searchQuery) {
        Collection<NodeInfo> nodes = listNodes();
        return filterApi.listNodes(nodes, searchQuery);
    }


}
