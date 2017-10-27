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

package com.codeabovelab.dm.cluman.ui.msg;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetContainersArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetEventsArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetLogContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetStatisticsArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ProcessEvent;
import com.codeabovelab.dm.cluman.cluster.docker.model.DockerEvent;
import com.codeabovelab.dm.cluman.ds.clusters.ClusterUtils;
import com.codeabovelab.dm.cluman.ds.nodes.NodeRegistration;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.events.EventsUtils;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.security.DockerServiceSecurityWrapper;
import com.codeabovelab.dm.cluman.ui.model.UIStatistics;
import com.codeabovelab.dm.common.mb.Subscriptions;
import com.codeabovelab.dm.common.security.Action;
import com.codeabovelab.dm.cluman.security.TempAuth;
import com.codeabovelab.dm.common.utils.Closeables;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 */
@Slf4j
@Component
class EventSources {

    private static final long TIMEOUT = 60_000L;
    private final DiscoveryStorage clusterStorage;
    private final NodeStorage nodeStorage;
    private final Lock lock = new ReentrantLock();
    //store immutable map
    private final AtomicReference<Map<String, Subscriptions<?>>> subs = new AtomicReference<>(Collections.emptyMap());
    private final Map<String, Subscriptions<?>> systemSubs;
    private final Collection<AutoCloseable> close = new ArrayList<>();
    private ExecutorService executor;
    private volatile long lastUpdate;

    @SuppressWarnings("unchecked")
    @Autowired
    public EventSources(DiscoveryStorage clusterStorage,
                        NodeStorage nodeStorage,
                        Map<String, Subscriptions<?>> systemSubs) {
        this.clusterStorage = clusterStorage;
        this.nodeStorage = nodeStorage;
        this.systemSubs = new HashMap<>(systemSubs);
        addStats(this.systemSubs.get(DockerLogEvent.BUS), DockerLogEvent.BUS + "-stats", this::getDockerLogEventKey);
        addStats(this.systemSubs.get(EventsUtils.BUS_ERRORS), EventsUtils.BUS_ERRORS + "-stats", (e) -> {
            // so, this bus can has any event type therefore we may add other key factories here
            return this.getDockerLogEventKey(e);
        });
    }

    private Object getDockerLogEventKey(Object e) {
        if(!(e instanceof DockerLogEvent)) {
            return null;
        }
        DockerLogEvent de = (DockerLogEvent) e;
        ContainerBase container = de.getContainer();
        if (container != null) {
            // container is null for images
            return de.getType().getValue() + ":" + de.getCluster() + ":" + container.getName();
        }
        return de.getType().getValue() + ":" + de.getCluster();
    }

    private <T> void addStats(Subscriptions<T> subscriptions, String busId, Function<T, Object> keyFactory) {
        EventStatsCollector<T> statsCollector = new EventStatsCollector<>(busId, keyFactory);
        subscriptions.subscribe(statsCollector);
        this.systemSubs.put(statsCollector.getBusId(), statsCollector.getSubscriptions());
        this.close.add(() -> {
            subscriptions.unsubscribe(statsCollector);
            statsCollector.close();
        });
    }

    @PostConstruct
    public void init() {
        ThreadFactory tf = new ThreadFactoryBuilder().setDaemon(true).setNameFormat(getClass().getSimpleName() + "-%d").build();
        executor = Executors.newCachedThreadPool(tf);
        // load system subs, it fix issue: "Can not find Subscriptions: 'bus.cluman.errors'"
        subs.set(ImmutableMap.copyOf(systemSubs));
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
        close.forEach(Closeables::close);
    }


    private void load() {
        if(lastUpdate + TIMEOUT > System.currentTimeMillis()) {
            return;
        }
        if(!lock.tryLock()) {
            return;
        }
        try(TempAuth ta = TempAuth.asSystem()) {
            //we must inherit existed subscriptions fore prevent leaks
            Esuc esuc = new Esuc(subs.get());
            esuc.putAll(systemSubs);
            List<NodesGroup> clusters = clusterStorage.getClusters();
            for(NodesGroup ng: clusters) {
                if(!ClusterUtils.isDockerBased(ng) || !ng.getState().isOk()) {
                    continue;
                }
                String id = "cluster:" + ng.getName() + ":docker";
                //swarm produce events only after 1.2.4 version
                esuc.update(id, (i) -> makeDocker(ng.getDocker(), id));
            }
            Collection<NodeInfo> nodes = nodeStorage.getNodes((nr) -> true);
            for(NodeInfo ni: nodes) {
                processNode(esuc, ni);
            }
            subs.compareAndSet(esuc.getOldMap(), esuc.getNewMap());
            //close outdated subscriptions (do not put it in finally block)
            esuc.free();
            lastUpdate = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }

    private void processNode(Esuc esuc,
                                NodeInfo ni) {
        NodeRegistration nr = nodeStorage.getNodeRegistration(ni.getName());
        {
            String id = "node:" + ni.getName() + ":health";
            esuc.update(id, (i) -> nr.getHealthSubscriptions());
        }
        DockerService service = nr.getDocker();
        if(service == null || !service.isOnline()) {
            return;
        }
        {
            String id = "node:" + ni.getName() + ":docker";
            esuc.update(id, (i) -> makeDocker(service, id));
        }
        try {
            List<DockerContainer> containers = service.getContainers(new GetContainersArg(true));
            String clusterName = ni.getCluster();
            for(DockerContainer dc : containers) {
                String cidPrefix = "container:" + clusterName + ":" + dc.getName();
                String idPrefix = "container:" + dc.getId();
                updateContainerStdout(esuc, service, dc, cidPrefix + ":stdout");
                updateContainerStdout(esuc, service, dc, idPrefix + ":stdout");
                updateContainerStat(esuc, service, dc, cidPrefix + ":stat");
                updateContainerStat(esuc, service, dc, idPrefix + ":stat");
            }
        } catch (Exception e) {
            log.error("Error on node '{}'.", ni.getName(), e);
        }
    }

    private void updateContainerStdout(Esuc esuc,
                                       DockerService service,
                                       DockerContainer dc,
                                       String cid) {
        esuc.update(cid, (i) -> makeContainerStdout(service, dc, cid));
    }

    private void updateContainerStat(Esuc esuc,
                                     DockerService service,
                                     DockerContainer dc,
                                     String cid) {
        esuc.update(cid, (i) -> makeContainerStat(service, dc, cid));
    }

    private Subscriptions<?> makeContainerStat(DockerService service, DockerContainer dc, String cid) {
        LazySubscriptions.Builder<UIStatistics> builder = LazySubscriptions.builder(UIStatistics.class).id(cid);
        DockerMethodSubscriber.Builder<UIStatistics, GetStatisticsArg> dms = DockerMethodSubscriber.builder();
        dms.id(cid);
        dms.setExecutorService(this.executor);
        dms.setDocker(service);
        dms.argument((c) -> {
            //therefore we cannot check user access in docker (at now subscription doing under system rights)
            // we need to do it here
            if (service instanceof DockerServiceSecurityWrapper) {
                ((DockerServiceSecurityWrapper) service).checkContainerAccess(dc.getId(), Action.READ);
            }
            return GetStatisticsArg.builder()
              .stream(true)
              .id(dc.getId())
              .watcher((s) -> c.accept(UIStatistics.from(s))).build();
        });
        dms.method(service::getStatistics);
        builder.subscriber(dms.build());
        return builder.build();
    }

    private Subscriptions<?> makeContainerStdout(DockerService service, DockerContainer dc, String cid) {
        LazySubscriptions.Builder<ProcessEvent> builder = LazySubscriptions.builder(ProcessEvent.class).id(cid);
        DockerMethodSubscriber.Builder<ProcessEvent, GetLogContainerArg> dms = DockerMethodSubscriber.builder();
        dms.id(cid);
        dms.setExecutorService(this.executor);
        dms.setDocker(service);
        dms.argument((c) -> {
            //therefore we cannot check user access in docker (at now subscription doing under system rights)
            // we need to do it here
            if (service instanceof DockerServiceSecurityWrapper) {
                ((DockerServiceSecurityWrapper) service).checkContainerAccess(dc.getId(), Action.READ);
            }
            return GetLogContainerArg.builder()
              .follow(true)
              .id(dc.getId())
              .tail(10)
              .stderr(true)
              .stdout(true)
              .timestamps(true)
              .watcher(c).build();
        });
        dms.method(service::getContainerLog);
        builder.subscriber(dms.build());
        return builder.build();
    }

    private Subscriptions<?> makeDocker(DockerService service, String id) {
        LazySubscriptions.Builder<DockerEvent> builder = LazySubscriptions.builder(DockerEvent.class).id(id);
        DockerMethodSubscriber.Builder<DockerEvent, GetEventsArg> dms = DockerMethodSubscriber.builder();
        dms.id(id);
        dms.setExecutorService(this.executor);
        dms.setDocker(service);
        dms.argument((c) -> {
            if (service instanceof DockerServiceSecurityWrapper) {
                ((DockerServiceSecurityWrapper) service).checkServiceAccess(Action.READ);
            }
            return GetEventsArg.builder().watcher(c).build();
        });
        dms.method(service::subscribeToEvents);
        builder.subscriber(dms.build());
        return builder.build();
    }

    public Collection<String> list() {
        load();
        ArrayList<String> list = new ArrayList<>(getSubs().keySet());
        list.sort(null);
        return Collections.unmodifiableList(list);
    }

    public Subscriptions<?> get(String id) {
        load();
        return getSubs().get(id);
    }

    private Map<String, Subscriptions<?>> getSubs() {
        return subs.get();
    }
}
