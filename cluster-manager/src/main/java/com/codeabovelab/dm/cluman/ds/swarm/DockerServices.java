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
import com.codeabovelab.dm.cluman.cluster.docker.HttpAuthInterceptor;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerServiceEvent;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerServiceImpl;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetEventsArg;
import com.codeabovelab.dm.cluman.cluster.docker.model.Actor;
import com.codeabovelab.dm.cluman.cluster.docker.model.DockerEvent;
import com.codeabovelab.dm.cluman.cluster.docker.model.EventType;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.ds.DockerServiceRegistry;
import com.codeabovelab.dm.cluman.ds.container.ContainerRegistration;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.security.AccessContextFactory;
import com.codeabovelab.dm.cluman.security.DockerServiceSecurityWrapper;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.cluman.security.TempAuth;
import com.codeabovelab.dm.common.utils.Throwables;
import com.codeabovelab.dm.platform.http.async.NettyRequestFactory;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.AsyncRestTemplate;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Registry for docker service. It hold and provide swarm and docker services. It does not provide virtual services,
 * therefore you must use {@link NodesGroup#getDocker()} directly. <p/>
 */
@Component
public class DockerServices implements DockerServiceRegistry, NodeRegistry {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ConcurrentMap<String, DockerService> clusters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DockerService> nodes = new ConcurrentHashMap<>();
    private final RegistryRepository registryRepository;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ScheduledExecutorService scheduledExecutor;
    private final ExecutorService executor;
    private final SwarmProcesses swarmProcesses;
    private final NodeInfoProvider nodeInfoProvider;
    private final MessageBus<DockerLogEvent> dockerEventMessageBus;
    private final DockerEventsConfig dockerMonitoringConfig;
    private final ContainerStorage containerStorage;
    private final MessageBus<DockerServiceEvent> dockerServiceEventMessageBus;
    private final AccessContextFactory aclContextFactory;
    private final Map<String, ScheduledFuture> watchingFutures = new ConcurrentHashMap<>();

    @Autowired
    public DockerServices(DockerServicesConfig configuration,
                          ContainerStorage containerStorage,
                          SwarmProcesses swarmProcesses,
                          RegistryRepository registryRepository,
                          NodeInfoProvider nodeInfoProvider,
                          DockerEventsConfig dockerMonitoringConfig,
                          AccessContextFactory aclContextFactory,
                          @Qualifier(NodeEvent.BUS) MessageBus<NodeEvent> nodeInfoMessageBus,
                          @Qualifier(DockerLogEvent.BUS) MessageBus<DockerLogEvent> dockerEventMessageBus,
                          @Qualifier(DockerServiceEvent.BUS) MessageBus<DockerServiceEvent> dockerServiceEventMessageBus) {
        this.containerStorage = containerStorage;
        this.registryRepository = registryRepository;
        this.swarmProcesses = swarmProcesses;
        this.nodeInfoProvider = nodeInfoProvider;
        this.dockerEventMessageBus = dockerEventMessageBus;
        this.dockerMonitoringConfig = dockerMonitoringConfig;
        this.dockerServiceEventMessageBus = dockerServiceEventMessageBus;
        this.aclContextFactory = aclContextFactory;
        nodeInfoMessageBus.subscribe((e) -> {
            switch (e.getAction()) {
                case StandardActions.UPDATE:
                case StandardActions.CREATE: {
                    registerNode(e.getCurrent());
                    break;
                }
                case StandardActions.DELETE: {
                    //TODO unregisterNode(e.getNode());
                    break;
                }
            }
        });
        String classPrefix = getClass().getSimpleName();
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat(classPrefix + "-executor-%d")
          .setUncaughtExceptionHandler(Throwables.uncaughtHandler(log))
          .build());
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(classPrefix + "-scheduled-%d")
                .setUncaughtExceptionHandler(Throwables.uncaughtHandler(log))
                .build());
        scheduledExecutor.scheduleWithFixedDelay(this::updateInfo,
                configuration.getRefreshInfoSeconds(),
                configuration.getRefreshInfoSeconds(),
                TimeUnit.SECONDS);
        scheduledExecutorService = Executors.newScheduledThreadPool(dockerMonitoringConfig.getCountOfThreads(), new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(classPrefix + "-eventsFetcher-%d")
                .setUncaughtExceptionHandler(Throwables.uncaughtHandler(log))
                .build());
        dockerServiceEventMessageBus.asSubscriptions().subscribe(this::serviceListener);
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
            return getNodeService(val);
        }
        //unknown type
        return null;
    }

    /**
     * @param instanceId id of swarm or docker node
     * @return
     */
    @Override
    public DockerService getService(String instanceId) {
        DockerService service = clusters.get(instanceId);
        return service;
    }

    /**
     * Return service by node name
     *
     * @param nodeName
     * @return
     */
    @Override
    public DockerService getNodeService(String nodeName) {
        DockerService service = nodes.get(nodeName);
        return service;
    }

    @Override
    public Set<String> getNodeServices() {
        return ImmutableSet.copyOf(nodes.keySet());
    }

    /**
     * service of node which is owns container.
     *
     * @param containerId
     * @return
     */
    public DockerService getServiceByContainer(String containerId) {
        ContainerRegistration container = containerStorage.getContainer(containerId);
        ExtendedAssert.notFound(container, "Can't find container by id " + containerId);
        DockerService dockerService = nodes.get(container.getNode());
        return dockerService;
    }

    public void registerNode(Node node) {
        registerNode(node.getName(), node.getAddress());
    }

    public DockerService registerNode(String nodeName, String address) {
        // we intentionally register node without specifying cluster
        ClusterConfig config = configForNode(address).build();

        Function<String, DockerService> factory = (nn) -> createDockerService(config, (b) -> b.setNode(nn));


        // also we register services by its containers
        final DockerService service = registerNodeBy(config, factory, nodeName);
        if (service != null) {
            watchingFutures.computeIfAbsent(nodeName, s -> {
                log.info("try to register node for fetching logs {}", nodeName);

                return scheduledExecutorService.scheduleAtFixedRate(() -> {
                        Long time = new Date().getTime();
                        Long afterTime = time + dockerMonitoringConfig.getPeriodInSeconds() * 1000;
                        GetEventsArg getEventsArg = GetEventsArg.builder()
                          .since(time)
                          .until(afterTime)
                          .watcher(e -> dockerEventMessageBus.accept(convertToLogEvent(nodeName, e)))
                          .build();
                        log.debug("getting events args {}", getEventsArg);
                        try (TempAuth ta = TempAuth.asSystem()) {
                            service.subscribeToEvents(getEventsArg);
                        }
                    },
                    dockerMonitoringConfig.getInitialDelayInSeconds(),
                    dockerMonitoringConfig.getPeriodInSeconds(), TimeUnit.SECONDS);

            });
        }
        return service;
    }

    private DockerLogEvent convertToLogEvent(final String nodeName, final DockerEvent e) {
        // see https://docs.docker.com/engine/reference/commandline/events/
        DockerLogEvent.Builder logEvent = DockerLogEvent.builder();
        final String action = e.getAction();
        logEvent.setAction(action);
        final EventType type = e.getType();
        if (type == EventType.CONTAINER) {
            ContainerBase.Builder builder = ContainerBase.builder();
            builder.setId(e.getId());
            Actor actor = e.getActor();
            Map<String, String> attributes = actor.getAttributes();
            builder.setLabels(attributes);
            //remove attributes which is not a labels.
            builder.getLabels().keySet().removeAll(ImmutableSet.of("name", "image"));
            builder.setName(attributes.get("name"));
            builder.setImage(e.getFrom());
            logEvent.setContainer(builder.build());
            switch (action) {
                //we do not support 'kill' action
                case "kill": logEvent.setAction(StandardActions.STOP);
                    break;
                case "destroy":logEvent.setAction(StandardActions.DELETE);
                    break;
            }
        }
        logEvent.setDate(new Date(e.getTime() * 1000L));
        String localNodeName = (e.getNode() != null) ? e.getNode().getName() : nodeName;
        logEvent.setNode(localNodeName);
        logEvent.setCluster(this.nodeInfoProvider.getNodeCluster(localNodeName));
        logEvent.setType(type);
        logEvent.setStatus(e.getStatus());
        logEvent.setSeverity(calculateSeverity(e.getStatus()));
        return logEvent.build();
    }

    private Severity calculateSeverity(String status) {
        switch (status) {
            case "die":
                return Severity.ERROR;
            case "kill":
                return Severity.WARNING;
            default:
                return Severity.INFO;
        }
    }

    private DockerService registerNodeBy(ClusterConfig config, Function<String, DockerService> factory, String name) {
        if (name == null) {
            return null;
        }
        int i = 10;
        while (i > 0) {
            DockerService service = nodes.computeIfAbsent(name, factory);
            if (config.getHost().equals(service.getClusterConfig().getHost())) {
                return service;
            }
            nodes.remove(name, service);
            i--;
        }
        Assert.isTrue(i > 0, "Detect cycling on register node.");
        return null;
    }

    private DockerService createDockerService(ClusterConfig clusterConfig, Consumer<DockerServiceImpl.Builder> dockerConsumer) {
        DockerServiceImpl.Builder b = DockerServiceImpl.builder();
        b.setConfig(clusterConfig);
        String cluster = clusterConfig.getCluster();
        if(cluster != null) {
            b.setCluster(cluster);
        }
        b.setRestTemplate(createNewRestTemplate());
        b.setEventConsumer(this::dockerEventConsumer);
        b.setNodeInfoProvider(nodeInfoProvider);
        if (dockerConsumer != null) {
            dockerConsumer.accept(b);
        }
        DockerService ds = b.build();
        ds = securityWrapper(ds);
        return ds;
    }

    private void dockerEventConsumer(DockerServiceEvent dockerServiceEvent) {
        executor.execute(() -> {
            try(TempAuth auth = TempAuth.asSystem()) {
                dockerServiceEventMessageBus.accept(dockerServiceEvent);
            }
        });
    }

    public DockerService getOrCreateCluster(ClusterConfig clusterConfig, Consumer<DockerServiceImpl.Builder> dockerConsumer) {
        return clusters.computeIfAbsent(clusterConfig.getCluster(), (cid) -> {
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
            DockerService dockerService = createDockerService(instanceConfig, dockerConsumer);
            // here we have conceptual problem: swarm service is 'start', but DockerService is 'create',
            // which action we must send into bus? So it may be service of long ago created cluster, therefore we use 'start'.
            dockerServiceEventMessageBus.accept(new DockerServiceEvent(dockerService, StandardActions.START));
            return dockerService;
        });
    }

    /**
     * Make address part of docker service. Note that it leave cluster id with null value.
     *
     * @param addr address of service
     * @return
     */
    private ClusterConfigImpl.Builder configForNode(String addr) {
        return ClusterConfigImpl.builder()
                .host(addr);
    }


    private void serviceListener(DockerServiceEvent e) {
        try {
            if (e instanceof DockerServiceEvent.DockerServiceInfoEvent) {
                DockerServiceEvent.DockerServiceInfoEvent ie = (DockerServiceEvent.DockerServiceInfoEvent) e;
                for (NodeInfo ni : ie.getInfo().getNodeList()) {
                    registerNode(ni);
                }
            }
        } catch (Exception ex) {
            log.error("On event: {}", e, ex);
        }
    }

    private AsyncRestTemplate createNewRestTemplate() {
        // we use async client because usual client does not allow to interruption in some cases
        AsyncClientHttpRequestFactory factory = new NettyRequestFactory();
        final AsyncRestTemplate restTemplate = new AsyncRestTemplate(factory);
        restTemplate.setInterceptors(Collections.singletonList(new HttpAuthInterceptor(registryRepository)));
        return restTemplate;
    }

    @Override
    public Set<String> getServices() {
        return ImmutableSet.copyOf(clusters.keySet());
    }

    @PreDestroy
    public void shutdown() {
        scheduledExecutor.shutdown();
        scheduledExecutorService.shutdown();
    }

    public DockerService securityWrapper(DockerService dockerService) {
        return new DockerServiceSecurityWrapper(aclContextFactory, dockerService);
    }
}
