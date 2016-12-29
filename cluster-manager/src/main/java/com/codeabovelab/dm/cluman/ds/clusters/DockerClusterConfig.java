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

package com.codeabovelab.dm.cluman.ds.clusters;

import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.SwarmSpec;
import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.codeabovelab.dm.common.utils.Cloneables;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Do not confuse with {@link SwarmNodesGroupConfig} because it may contain part of {@link SwarmSpec}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DockerClusterConfig extends AbstractNodesGroupConfig<DockerClusterConfig> implements DockerBasedClusterConfig {
    @KvMapping
    private ClusterConfigImpl config;
    @KvMapping
    private int swarmPort = 4375;
    /**
     * List of managers nodes.
     */
    @KvMapping
    private List<String> managers;

    @Override
    public DockerClusterConfig clone() {
        DockerClusterConfig clone = super.clone();
        clone.managers = Cloneables.clone(clone.managers);
        return clone;
    }
}
