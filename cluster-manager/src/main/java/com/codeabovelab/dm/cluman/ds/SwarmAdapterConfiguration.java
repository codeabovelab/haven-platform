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

package com.codeabovelab.dm.cluman.ds;

import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl;
import com.codeabovelab.dm.cluman.ds.clusters.DiscoveryStorageImpl;
import com.codeabovelab.dm.cluman.ds.clusters.SwarmNodesGroupConfig;
import com.codeabovelab.dm.cluman.ds.nodes.DiscoveryNodeController;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServicesConfig;
import com.codeabovelab.dm.cluman.ds.swarm.SwarmProcessesConfig;
import com.codeabovelab.dm.cluman.model.DockerLogEvent;
import com.codeabovelab.dm.cluman.model.NodesGroupEvent;
import com.codeabovelab.dm.cluman.model.NodeEvent;
import com.codeabovelab.dm.common.mb.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * Configuration for swarm service
 */
@Configuration
@EnableConfigurationProperties({SwarmsConfig.class, DockerServicesConfig.class, SwarmProcessesConfig.class})
@ComponentScan(basePackageClasses = {DiscoveryNodeController.class})
public class SwarmAdapterConfiguration implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private ObjectFactory<DiscoveryStorageImpl> storageFactory;

    @Autowired
    private SwarmsConfig swarmsConfig;

    @Autowired
    private DockerServices dockerServices;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Map<String, ClusterConfigImpl.Builder> configs = swarmsConfig.getConfigs();
        DiscoveryStorageImpl storage = storageFactory.getObject();
        if(CollectionUtils.isEmpty(configs)) {
            LoggerFactory.getLogger(getClass()).info("No configs in " + swarmsConfig);
        } else {
            for(Map.Entry<String, ClusterConfigImpl.Builder> e: configs.entrySet()) {
                String cluster = e.getKey();
                ClusterConfigImpl.Builder config = ClusterConfigImpl.builder().from(e.getValue());
                config.cluster(cluster);
                SwarmNodesGroupConfig ngc = new SwarmNodesGroupConfig();
                ngc.setConfig(config.build());
                ngc.setName(cluster);
                storage.getOrCreateGroup(ngc);
            }
        }
        storage.load();
    }

    @Configuration
    public static class PrerequestConfiguration {

        @Bean(name = NodeEvent.BUS)
        MessageBus<NodeEvent> nodeMessageBus() {
            return MessageBuses.create(NodeEvent.BUS, NodeEvent.class);
        }

        @Bean(name = DockerLogEvent.BUS)
        MessageBus<DockerLogEvent> dockerMessageBus() {
            return MessageBuses.create(DockerLogEvent.BUS, DockerLogEvent.class);
        }

        @Bean(name = NodesGroupEvent.BUS)
        MessageBus<NodesGroupEvent> nodesGroupMessageBus() {
            return MessageBuses.create(NodesGroupEvent.BUS, NodesGroupEvent.class);
        }

    }

}
