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

package com.codeabovelab.dm.cluman.ds.nodes;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetEventsArg;
import com.codeabovelab.dm.cluman.cluster.docker.model.Actor;
import com.codeabovelab.dm.cluman.cluster.docker.model.DockerEvent;
import com.codeabovelab.dm.cluman.cluster.docker.model.EventType;
import com.codeabovelab.dm.cluman.ds.swarm.DockerEventsConfig;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.persistent.PersistentBusFactory;
import com.codeabovelab.dm.cluman.security.AccessContextFactory;
import com.codeabovelab.dm.cluman.security.SecuredType;
import com.codeabovelab.dm.cluman.security.TempAuth;
import com.codeabovelab.dm.common.mb.*;
import com.codeabovelab.dm.common.security.Action;
import com.codeabovelab.dm.common.utils.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 */
@Slf4j
class NodeRegistrationImpl implements NodeRegistration, AutoCloseable {

    private final String name;
    private final Object lock = new Object();
    private volatile NodeInfoImpl cache;
    private final NodeInfoImpl.Builder builder;

    private final MessageBus<NodeHealthEvent> healthBus;
    private volatile int ttl;
    private final ObjectIdentity oid;
    private volatile DockerService docker;
    private final NodeStorage nodeStorage;
    private final ScheduledExecutorService logFetcher;
    private volatile ScheduledFuture<?> logFuture;

    NodeRegistrationImpl(NodeStorage nodeStorage, PersistentBusFactory pbf, NodeInfo nodeInfo) {
        String name = nodeInfo.getName();
        NodeUtils.checkName(name);
        this.name = name;
        this.nodeStorage = nodeStorage;
        this.oid = SecuredType.NODE.id(name);
        // name may contain dots
        this.healthBus = pbf.create(NodeHealthEvent.class, "node[" + name + "].metrics", 2000/* TODO in config */);
        synchronized (lock) {
            this.builder = NodeInfoImpl.builder(nodeInfo);
        }
        this.logFetcher = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("node-"+name +"-log-fetcher-%d")
          .setUncaughtExceptionHandler(Throwables.uncaughtHandler(log, "Uncaught exception in '" + name + "' node log fetcher."))
          .build());
    }

    void init() {
        renewDocker();
    }

    /**
     * Time while node is actual, in seconds.
     * @param ttl time in seconds
     */
    public void setTtl(int ttl) {
        final int min = nodeStorage.getStorageConfig().getMinTtl();
        if(ttl < min) {
            ttl = min;
        }
        synchronized (lock) {
            this.ttl = ttl;
        }
    }

    public int getTtl() {
        synchronized (lock) {
            return this.ttl;
        }
    }

    @Override
    public Subscriptions<NodeHealthEvent> getHealthSubscriptions() {
        return this.healthBus.asSubscriptions();
    }

    private boolean isOn() {
        DockerService service = this.getDocker();
        return service != null && service.isOnline();
    }

    @Override
    public NodeInfoImpl getNodeInfo() {
        NodeInfoImpl ni;
        NodeInfoImpl old;
        final boolean onlineChanged;
        synchronized (lock) {
            builder.name(name);
            boolean on = isOn();
            onlineChanged = on != builder.isOn();
            old = ni = cache;
            if(ni == null || onlineChanged) {
                ni = cache = builder.on(on).build();
            }
        }
        if(onlineChanged) {
            fireNodeChanged(ni.isOn() ? NodeEvent.Action.ONLINE : NodeEvent.Action.OFFLINE, old, ni);
        }
        return ni;
    }

    private void fireNodeChanged(NodeEvent.Action action, NodeInfoImpl old, NodeInfoImpl ni) {
        this.nodeStorage.fireNodeModification(this, action, old, ni);
    }

    NodeMetrics updateHealth(NodeMetrics metrics) {
        checkAccessUpdate();
        NodeMetrics nmnew;
        String cluster;
        synchronized (lock) {
            nmnew = NodeMetrics.builder().from(this.builder.getHealth()).fromNonNull(metrics).build();
            this.builder.setHealth(nmnew);
            cluster = this.builder.getCluster();
            cache = null;
        }
        this.healthBus.accept(new NodeHealthEvent(this.name, cluster, nmnew));
        return nmnew;
    }

    private void checkAccessUpdate() {
        AccessContextFactory.getLocalContext().assertGranted(oid, Action.UPDATE);
    }

    /**
     * Update internal node info.
     */
    void updateNodeInfo(Consumer<NodeInfoImpl.Builder> modifier) {
        checkAccessUpdate();
        NodeMetrics nmnew = null;
        String cluster;
        NodeInfoImpl oldni;
        NodeInfoImpl ni;
        synchronized (lock) {
            oldni = getNodeInfo();
            NodeMetrics oldMetrics = this.builder.getHealth();
            boolean on = this.builder.isOn();
            NodeInfoImpl.Builder copy = NodeInfoImpl.builder(this.builder);
            modifier.accept(copy);
            validate(copy);
            boolean cancel = nodeStorage.fireNodePreModification(oldni, copy.build());
            if(cancel) {
                return;
            }
            this.builder.from(copy);
            NodeMetrics newMetrics = this.builder.getHealth();
            if(!Objects.equals(oldMetrics, newMetrics)) {
                nmnew = newMetrics;
            }
            this.builder.setOn(on);//we must save 'on' flag
            //refresh address if need
            this.setAddress(this.builder.getAddress());
            cluster = this.builder.getCluster();
            cache = null;
            ni = getNodeInfo();
        }
        if(!Objects.equals(oldni, ni)) {//we try to reduce count of unnecessary 'update' events
            fireNodeChanged(NodeEvent.Action.UPDATE, oldni, ni);
        }
        if(nmnew != null) {
            this.healthBus.accept(new NodeHealthEvent(this.name, cluster, nmnew));
        }
    }

    private void validate(NodeInfoImpl.Builder copy) {
        Assert.isTrue(Objects.equals(copy.getName(), this.name),
          "Wrong name of modified node: " + copy.getName() + ", when must be: " + this.name);
    }

    public void setCluster(String cluster) {
        update(this.builder::getCluster, this.builder::setCluster, cluster);
    }

    private <T> void update(Supplier<T> getter, Consumer<T> setter, T value) {
        NodeInfoImpl ni = null;
        NodeInfoImpl oldInfo;
        synchronized (lock) {
            oldInfo = cache;
            T oldVal = getter.get();
            if(!Objects.equals(oldVal, value)) {
                setter.accept(value);
                cache = null;
                ni = getNodeInfo();
                boolean cancel = true;
                try {
                    cancel = nodeStorage.fireNodePreModification(oldInfo, ni);
                } finally {
                    if(cancel) {
                        // it not good practice, and require that setter must be 'safe'
                        setter.accept(oldVal);
                        cache = null;
                        ni = null;
                    }
                }
            }
        }
        if(ni != null) {
            fireNodeChanged(NodeEvent.Action.UPDATE, oldInfo, ni);
        }
    }

    public String getCluster() {
        synchronized (lock) {
            return this.builder.getCluster();
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public ObjectIdentity getOid() {
        return oid;
    }

    @Override
    public DockerService getDocker() {
        synchronized (lock) {
            return docker;
        }
    }

    /**
     * It change address of node, that cause some side effects: recreation of DockerService for example.
     * @param address new address of node or null
     * @return new docker service, or old when address same as old
     */
    DockerService setAddress(String address) {
        synchronized (lock) {
            update(this.builder::getAddress, this.builder::setAddress, address);
            if(docker != null && docker.getAddress().equals(address)) {
                return getDocker();
            }
            renewDocker();
            return getDocker();
        }
    }

    private void renewDocker() {
        unsubscribe();
        this.docker = null;
        if(this.builder.getAddress() != null) {
            this.docker = this.nodeStorage.createNodeService(this);
            subscribe();
        }
    }

    private void subscribe() {
        DockerEventsConfig cfg = nodeStorage.getDockerEventConfig();
        final int periodInSeconds = cfg.getPeriodInSeconds();
        log.info("Register log fetcher from {} node, repeat every {} seconds", name, periodInSeconds);
        Assert.isNull(this.logFuture, "Future of docker logging is not null");
        this.logFuture = logFetcher.scheduleAtFixedRate(() -> {
            // we must handle errors here, because its may stop of task scheduling
            // for example - temporary disconnect of node will breaks logs fetching due to system restart
              try {
                  Long time = System.currentTimeMillis();
                  Long afterTime = time + periodInSeconds * 1000L;
                  GetEventsArg getEventsArg = GetEventsArg.builder()
                    .since(time)
                    .until(afterTime)
                    .watcher(this::proxyDockerEvent)
                    .build();
                  log.debug("getting events args {}", getEventsArg);
                  try (TempAuth ta = TempAuth.asSystem()) {
                      docker.subscribeToEvents(getEventsArg);
                  }
              } catch (Exception e) {
                  log.error("Can not fetch logs from {}, try again after {} seconds, error: {}", name, periodInSeconds, e.toString());
              }
          },
          cfg.getInitialDelayInSeconds(),
          periodInSeconds, TimeUnit.SECONDS);
    }

    private void proxyDockerEvent(DockerEvent e) {
        try {
            log.debug("Node '{}' send log event: {}", name, e);
            DockerLogEvent logEvent = convertToLogEvent(e);
            nodeStorage.acceptDockerLogEvent(logEvent);
        } catch (Exception ex) {
            log.error("can not convert {}", e, ex);
        }
    }

    private DockerLogEvent convertToLogEvent(final DockerEvent e) {
        // see https://docs.docker.com/engine/reference/commandline/events/
        DockerLogEvent.Builder logEvent = DockerLogEvent.builder();
        final String action = e.getAction();
        logEvent.setAction(action);
        String localNodeName = (e.getNode() != null) ? e.getNode().getName() : this.name;
        final EventType type = e.getType();
        if (type == EventType.CONTAINER) {
            ContainerBase.Builder builder = ContainerBase.builder();
            builder.setId(e.getId());
            builder.setNode(localNodeName);
            Actor actor = e.getActor();
            Map<String, String> attributes = actor.getAttributes();
            builder.setLabels(attributes);
            //remove attributes which is not a labels.
            builder.getLabels().keySet().removeAll(ImmutableSet.of("name", "image"));
            builder.setName(attributes.get("name"));
            builder.setImage(e.getFrom());
            DockerContainer.State state = null;
            // Containers report these events: attach, commit, copy, create, destroy, detach, die, exec_create,
            // exec_detach, exec_start, export, kill, oom, pause, rename, resize, restart, start,
            // stop, top, unpause, update
            switch (action) {
                //we do not support 'kill' action
                case "kill":
                    logEvent.setAction(StandardActions.STOP);
                    state = DockerContainer.State.EXITED;
                    break;
                case "destroy":
                    logEvent.setAction(StandardActions.DELETE);
                    state = DockerContainer.State.REMOVING;
                    break;
                case "die":
                case "stop":
                    state = DockerContainer.State.DEAD;
                    break;
                case "unpause":
                case "start":
                    state = DockerContainer.State.RUNNING;
                    break;
                case "pause":
                    state = DockerContainer.State.PAUSED;
                    break;
                case "restart":
                    state = DockerContainer.State.RESTARTING;
                    break;
            }
            builder.setState(state);
            logEvent.setContainer(builder.build());
        }
        logEvent.setDate(new Date(e.getTime() * 1000L));
        logEvent.setNode(localNodeName);
        String eventCluster = nodeStorage.getNodeCluster(localNodeName);
        String thisCluster = getCluster();
        if(!Objects.equals(thisCluster, eventCluster)) {
            log.warn("Current node cluster '{}' differ from event cluster '{}'", thisCluster, eventCluster);
        }
        logEvent.setCluster(eventCluster);
        logEvent.setType(type);
        logEvent.setStatus(e.getStatus());
        logEvent.setSeverity(calculateSeverity(e.getStatus()));
        return logEvent.build();
    }

    private Severity calculateSeverity(String status) {
        if(status == null) {
            return Severity.INFO;
        }
        switch (status) {
            case "die":
                return Severity.ERROR;
            case "kill":
                return Severity.WARNING;
            default:
                return Severity.INFO;
        }
    }


    private void unsubscribe() {
        ScheduledFuture<?> future = this.logFuture;
        this.logFuture = null;
        if(future != null) {
            future.cancel(true);
        }
    }

    String getAddress() {
        synchronized (lock) {
            return this.builder.getAddress();
        }
    }

    @Override
    public void close() throws Exception {
        unsubscribe();
        this.logFetcher.shutdownNow();
    }
}
