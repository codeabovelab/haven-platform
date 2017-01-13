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
import lombok.Data;
import org.springframework.util.Assert;

import java.util.function.Consumer;

/**
 */
@Data
public class ClusterCreationContext {
    private final ClusterFactory factory;
    private final String cluster;
    private Consumer<AbstractNodesGroup<?>> beforeClusterInit;

    ClusterCreationContext(ClusterFactory factory, String cluster) {
        this.factory = factory;
        this.cluster = cluster;
    }

    void beforeClusterInit(AbstractNodesGroup<?> cluster) {
        if(beforeClusterInit != null) {
            beforeClusterInit.accept(cluster);
        }
    }

    public AbstractNodesGroupConfig<?> createConfig(String type) {
        Assert.notNull(type, "type is null");
        AbstractNodesGroupConfig<?> config;
        switch (type) {
            case NodesGroupConfig.TYPE_SWARM: {
                SwarmNodesGroupConfig local;
                local = new SwarmNodesGroupConfig();
                config = local;
                break;
            }
            case NodesGroupConfig.TYPE_DOCKER: {
                config = new DockerClusterConfig();
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported type of cluster: " + type);
        }
        if(config instanceof DockerBasedClusterConfig) {
            factory.initDefaultConfig((DockerBasedClusterConfig) config);
        }
        config.setName(getCluster());
        return config;
    }
}
