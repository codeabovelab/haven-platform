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

import com.codeabovelab.dm.cluman.ds.clusters.ClusterUtils;
import com.codeabovelab.dm.cluman.ds.clusters.DefaultNodesGroupConfig;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import com.codeabovelab.dm.cluman.model.NodeInfo;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.cluman.ui.model.UiNodeGroup;
import com.codeabovelab.dm.common.security.Authorities;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Api for environments.
 */
@RestController
@RequestMapping(value = "/ui/api/groups", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NodeGroupApi {

    private final NodeStorage nodeStorage;
    private final DiscoveryStorage discoveryStorage;

    @RequestMapping(value = "/", method = GET)
    public List<UiNodeGroup> list() {
        List<NodesGroup> clusters = this.discoveryStorage.getClusters();
        List<UiNodeGroup> envs = clusters.stream()
          .filter((c) -> !ClusterUtils.isDockerBased(c))
          .map((e) -> fromCluster(e))
          .collect(Collectors.toList());
        return envs;
    }

    @Secured(Authorities.ADMIN_ROLE)
    @RequestMapping(value = "/{groupId}/nodes", method = POST)
    public void createGroupByNodeList(@PathVariable("groupId") String groupId, @RequestParam(value = "nodes") List<String> nodes) {
        this.discoveryStorage.getOrCreateGroup(new DefaultNodesGroupConfig(groupId, "list:" + String.join(", ", nodes)));
    }

    @Secured(Authorities.ADMIN_ROLE)
    @RequestMapping(value = "/{groupId}/filter", method = POST)
    public void createGroupByFilter(@PathVariable("groupId") String groupId, @RequestParam(value = "filter") String filter) {
        this.discoveryStorage.getOrCreateGroup(new DefaultNodesGroupConfig(groupId, filter));
    }

    @Secured(Authorities.ADMIN_ROLE)
    @RequestMapping(value = "/{groupId}", method = DELETE)
    public void deleteGroup(@PathVariable("groupId") String groupId) {
        this.discoveryStorage.deleteNodeGroup(groupId);
    }

    private UiNodeGroup fromCluster(NodesGroup cluster) {
        UiNodeGroup env = new UiNodeGroup();
        final String name = cluster.getName();
        env.setName(name);
        env.setFilter(cluster.getImageFilter());
        return env;
    }

    @RequestMapping(value = "/{group}/nodes", method = GET)
    public Collection<NodeInfo> listNodes(@PathVariable("group") String env) {

        Collection<NodeInfo> nodes = nodeStorage.getNodes((ni) -> true);
        return nodes;
    }

}
