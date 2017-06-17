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

package com.codeabovelab.dm.cluman.ds.swarm;

import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfig;
import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerServiceEvent;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerServiceImpl;
import com.codeabovelab.dm.cluman.ds.DockerServiceFactory;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.cluman.model.StandardActions;
import com.codeabovelab.dm.cluman.security.TempAuth;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.utils.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Registry for docker service. It hold and provide swarm and docker services. It does not provide virtual services,
 * therefore you must use {@link NodesGroup#getDocker()} directly. <p/>
 */
@Component
public class DockerServices {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ConcurrentMap<String, DockerService> clusters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutor;
    private final SwarmProcesses swarmProcesses;
    private final NodeStorage nodeStorage;

    private final MessageBus<DockerServiceEvent> dockerServiceEventMessageBus;
    private final DockerServiceFactory dockerFactory;

    @Autowired
    public DockerServices(DockerServicesConfig configuration,
                          SwarmProcesses swarmProcesses,
                          NodeStorage nodeStorage,
                          DockerServiceFactory dockerFactory,
                          @Qualifier(DockerServiceEvent.BUS) MessageBus<DockerServiceEvent> dockerServiceEventMessageBus) {
        this.swarmProcesses = swarmProcesses;
        this.nodeStorage = nodeStorage;
        this.dockerServiceEventMessageBus = dockerServiceEventMessageBus;
        this.dockerFactory = dockerFactory;

        String classPrefix = getClass().getSimpleName();

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(classPrefix + "-scheduled-%d")
                .setUncaughtExceptionHandler(Throwables.uncaughtHandler(log))
                .build());
        scheduledExecutor.scheduleWithFixedDelay(this::updateInfo,
                configuration.getRefreshInfoSeconds(),
                configuration.getRefreshInfoSeconds(),
                TimeUnit.SECONDS);
    }

    private void updateInfo() {
        // we call get info for periodically updating info cache,
        //  it need for actual info about nodes health, that we can only obtain trough swarm service
        try (TempAuth ta = TempAuth.asSystem()) {
            for (DockerService service : this.clusters.values()) {
                try {
                    service.getInfo();
                } catch (Exception e) {
                    log.error("While getInfo on {} ", service.getId(), e);
                }
            }
        }
    }


    /**
     * @see DockerService#getId()
     * @param id
     * @return
     */
    public DockerService getById(String id) {
        if(id == null || !id.startsWith(DockerService.DS_PREFIX)) {
            return null;
        }
        int off = DockerService.DS_PREFIX.length();
        int split = id.indexOf(':', off);
        String type = id.substring(off, split);
        String val = id.substring(split + 1);
        if("cluster".equals(type)) {
            return getService(val);
        }
        if("node".equals(type)) {
            return nodeStorage.getNodeService(val);
        }
        //unknown type
        return null;
    }

    /**
     * Do not use this for obtain cluster service.
     * @param instanceId id of swarm or docker node
     * @return docker service
     */
    public DockerService getService(String instanceId) {
        return clusters.get(instanceId);
    }

    /**
     * Return service by node name
     *
     * @param nodeName
     * @return
     */
    @Deprecated
    public DockerService getNodeService(String nodeName) {
        return nodeStorage.getNodeService(nodeName);
    }

    public DockerService getOrCreateCluster(ClusterConfig clusterConfig, Consumer<DockerServiceImpl.Builder> dockerConsumer) {
        String cluster = clusterConfig.getCluster();
        Assert.hasText(cluster, "Cluster field in config is null or empty");
        return clusters.computeIfAbsent(cluster, (cid) -> {
            ClusterConfig instanceConfig = clusterConfig;
            if (clusterConfig.getHost() == null) {
                // if no defined swarm hosts then we must create own swarm instance and run it
                SwarmProcesses.SwarmProcess process = swarmProcesses.addCluster(clusterConfig);
                process.waitStart();
                // so, we create new swarm process and now need to modify config with process address
                ClusterConfigImpl.Builder ccib = ClusterConfigImpl.builder(clusterConfig);
                ccib.host(process.getAddress());
                instanceConfig = ccib.build();
            }
            DockerService dockerService = dockerFactory.createDockerService(instanceConfig, dockerConsumer);
            // here we have conceptual problem: swarm service is 'start', but DockerService is 'create',
            // which action we must send into bus? So it may be service of long ago created cluster, therefore we use 'start'.
            dockerServiceEventMessageBus.accept(new DockerServiceEvent(dockerService, StandardActions.START));
            return dockerService;
        });
    }

    public Set<String> getServices() {
        return ImmutableSet.copyOf(clusters.keySet());
    }

    @PreDestroy
    public void shutdown() {
        scheduledExecutor.shutdown();
    }

}
