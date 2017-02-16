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

package com.codeabovelab.dm.cluman.model;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.ds.clusters.AbstractNodesGroupConfig;
import com.codeabovelab.dm.cluman.ds.clusters.ClusterConfigFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Hold clusters
 */
public interface DiscoveryStorage {

    String GROUP_ID_ALL = "all";
    String GROUP_ID_ORPHANS = "orphans";

    Collection<String> SYSTEM_GROUPS = Arrays.asList(GROUP_ID_ALL, GROUP_ID_ORPHANS);

    /**
     * Return exists cluster or create new for concrete node.
     * @param nodeId id of node
     * @param clusterId default cluster id
     * @return exists cluster or create new.
     */
    NodesGroup getClusterForNode(String nodeId, String clusterId);

    /**
     * Return exists cluster of concrete node.
     * @param nodeId
     * @return cluter or null
     */
    NodesGroup getClusterForNode(String nodeId);

    /**
     * Return exists cluster or null
     * @param clusterId
     * @return exists cluster or null
     */
    NodesGroup getCluster(String clusterId);

    /**
     *  Return existed cluster or create new.
     * @param clusterId name of cluster
     * @param factory factory or null
     * @return NodesGroup, never null
     */
    NodesGroup getOrCreateCluster(String clusterId, ClusterConfigFactory factory);

    /**
     * Register new group, or return already registered. Like {@link #getOrCreateCluster(String, ClusterConfigFactory)} but allow to
     * create node group and real clusters too.
     * @param config
     * @return registered node group.
     */
    NodesGroup getOrCreateGroup(AbstractNodesGroupConfig<?> config);

    void deleteCluster(String clusterId);

    void deleteNodeGroup(String clusterId);

    List<NodesGroup> getClusters();

    DockerService getService(String name);

    Set<String> getServices();
}
