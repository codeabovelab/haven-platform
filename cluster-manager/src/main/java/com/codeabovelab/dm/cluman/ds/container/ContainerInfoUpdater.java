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

package com.codeabovelab.dm.cluman.ds.container;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerServiceEvent;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetContainersArg;
import com.codeabovelab.dm.cluman.cluster.docker.model.EventType;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.common.mb.Subscriptions;
import com.codeabovelab.dm.common.security.TempAuth;
import com.codeabovelab.dm.common.utils.RescheduledTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Utility which subscribe to different events and refresh container list. Also it refresh list o timeout. <p/>
 * NOTE: we do _not_ check node services to 'online' state in this class.
 */
@Slf4j
@Component
class ContainerInfoUpdater implements SmartLifecycle {
    private boolean started;
    private final DockerServices dockerServices;
    private final ContainerStorageImpl containerStorage;
    private final Subscriptions<NodeEvent> nodeSubs;
    private final Subscriptions<DockerServiceEvent> dockerSubs;
    private final Subscriptions<DockerLogEvent> dockerLogSubs;
    private final ConcurrentMap<String, RescheduledTask> scheduledNodes;
    private final ScheduledExecutorService scheduledService;

    @Autowired
    public ContainerInfoUpdater(DockerServices dockerServices,
                                ContainerStorageImpl containerStorage,
                                @Qualifier(NodeEvent.BUS) Subscriptions<NodeEvent> nodeSubs,
                                @Qualifier(DockerServiceEvent.BUS) Subscriptions<DockerServiceEvent> dockerSubs,
                                @Qualifier(DockerLogEvent.BUS) Subscriptions<DockerLogEvent> dockerLogSubs
                                ) {
        this.dockerServices = dockerServices;
        this.containerStorage = containerStorage;
        this.nodeSubs = nodeSubs;
        this.dockerSubs = dockerSubs;
        this.dockerLogSubs = dockerLogSubs;
        this.nodeSubs.subscribe(this::onNodeEvent);
        this.dockerSubs.subscribe(this::onDockerEvent);
        this.dockerLogSubs.subscribe(this::onDockerLogEvent);
        this.scheduledService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat(getClass().getSimpleName() + "-%d")
          .build());
        this.scheduledNodes = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void start() {
        this.started = true;
    }

    @Override
    public void stop() {
        if(!this.started) {
            return;
        }
        this.started = false;
    }

    @Override
    public boolean isRunning() {
        return started;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    private void onDockerLogEvent(DockerLogEvent dle) {
        if(dle.getType() != EventType.CONTAINER) {
            return;
        }
        final ContainerBase container = dle.getContainer();
        final String id = container.getId();
        switch(dle.getAction()) {
            case StandardActions.DELETE: {
                containerStorage.deleteContainer(id);
                break;
            }
            default: {
                String node = dle.getNode();
                // we can not create containers here because it not full filled
                ContainerRegistration cr = containerStorage.getContainer(id);
                if(cr == null) {
                    scheduleNodeUpdate(node);
                }
            }
        }
    }

    private void scheduleNodeUpdate(String node) {
        RescheduledTask task = this.scheduledNodes.computeIfAbsent(node, (n) -> {
            Runnable runnable = () -> this.updateNodeByName(n);
            return RescheduledTask.builder()
              .service(scheduledService)
              .runnable(runnable)
              .maxDelay(1L, TimeUnit.MINUTES)
              .build();
        });
        task.schedule(10L, TimeUnit.SECONDS);
    }

    private void onDockerEvent(DockerServiceEvent e) {
        String action = e.getAction();
        if(StandardActions.UPDATE.equals(action) || StandardActions.OFFLINE.equals(action)) {
            return;
        }
        String node = e.getNode();
        if(node == null) {
            return;
        }
        log.info("Node service '{}' is {}, schedule update containers.", node, action);
        scheduleNodeUpdate(node);
    }

    private void updateNodeByName(String node) {
        try(TempAuth ta = TempAuth.asSystem()) {
            DockerService service = dockerServices.getNodeService(node);
            if (service == null) {
                return;
            }
            // we must _not_ check service to 'online' here
            updateForNode(service);
        }
    }

    private void onNodeEvent(NodeEvent nodeEvent) {
        NodeInfo ni = nodeEvent.getNode();
        String name = ni.getName();
        String action = nodeEvent.getAction();
        if(StandardActions.OFFLINE.equals(action)) {
            log.info("Node '{}' offline remove containers.", name);
            containerStorage.removeNodeContainers(name);
            return;
        }
        // at first event 'ONLINE', node does not have a service, but we ignore second event
        // so we need to wait when docker service is registered
        if(StandardActions.ONLINE.equals(action)) {
            DockerService dockerService = dockerServices.getNodeService(name);
            // we do _not_ check service to 'online' here
            if(dockerService != null) {
                log.info("Node '{}' is online force update containers.", name);
                updateForNode(dockerService);
            }
        }
    }

    @Scheduled(fixedDelay = 5L * 60_000L /* 5 min */)
    public void update() {
        try(TempAuth ta = TempAuth.asSystem()) {
            log.info("Begin update containers list");
            for(String node: dockerServices.getNodeServices()) {
                DockerService nodeService = dockerServices.getNodeService(node);
                // we do _not_ check service to 'online' here
                if(nodeService == null) {
                    continue;
                }
                updateForNode(nodeService);
            }
            log.info("End update containers list");
        }
    }

    private void updateForNode(DockerService nodeService) {
        String node = nodeService.getNode();
        log.info("Update containers list of node '{}'", node);
        try {
            List<DockerContainer> containers = nodeService.getContainers(new GetContainersArg(true));
            Set<String> old = this.containerStorage.getContainersIdsByNode(node);
            for(DockerContainer dc: containers) {
                old.remove(dc.getId());
                this.containerStorage.getOrCreateContainer(dc, node);
            }
            this.containerStorage.remove(old);
            log.info("Containers of node '{}', current:{}, removed:{}", node, containers.size(), old.size());
        } catch (Exception e) {
            log.info("Updating containers of node '{}' failed with error.", node, e);
        }
    }
}
