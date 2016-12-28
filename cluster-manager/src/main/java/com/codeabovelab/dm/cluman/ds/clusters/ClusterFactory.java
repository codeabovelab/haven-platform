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

import com.codeabovelab.dm.cluman.model.NodesGroup;
import lombok.Data;
import org.springframework.util.Assert;

/**
 */
@Data
class ClusterFactory {
    private final DiscoveryStorageImpl storage;
    private AbstractNodesGroupConfig<?> config;
    private String type;
    private ClusterConfigFactory configFactory;

    public ClusterFactory config(SwarmNodesGroupConfig config) {
        setConfig(config);
        return this;
    }

    public ClusterFactory configFactory(ClusterConfigFactory consumer) {
        setConfigFactory(consumer);
        return this;
    }

    NodesGroup build(String clusterId) {
        ClusterCreationContext ccc = new ClusterCreationContext(clusterId);
        processConfig(ccc);
        AbstractNodesGroup<?> cluster;
        if(config instanceof SwarmNodesGroupConfig) {
            SwarmNodesGroupConfig localConfig = (SwarmNodesGroupConfig) config;
            cluster = SwarmCluster.builder().storage(storage).config(localConfig).build();
        } else if(config instanceof DockerClusterConfig) {
            DockerClusterConfig localConfig = (DockerClusterConfig) config;
            cluster = DockerCluster.builder().storage(storage).config(localConfig).build();
        } else {
            throw new IllegalArgumentException("Unsupported type of cluster config: " + config.getClass());
        }
        ccc.beforeClusterInit(cluster);
        cluster.init();
        return cluster;
    }

    private void processConfig(ClusterCreationContext ccc) {
        Assert.isTrue(type != null || config != null || configFactory != null,
          "Both 'type' and 'config' is null, we can not resolve type of created cluster.");
        if (config == null) {
            if(configFactory != null) {
                config = configFactory.create(ccc);
                Assert.notNull(config, "Config factory: " + configFactory + " return null.");
            } else {
                config = ccc.createConfig(getType());
            }
        }
    }
}
