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

import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfig;
import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerServiceEvent;
import com.codeabovelab.dm.cluman.ds.DockerServiceFactory;
import com.codeabovelab.dm.cluman.ds.SwarmUtils;
import com.codeabovelab.dm.cluman.ds.swarm.DockerEventsConfig;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.persistent.PersistentBusFactory;
import com.codeabovelab.dm.cluman.reconfig.ReConfigObject;
import com.codeabovelab.dm.cluman.reconfig.ReConfigurable;
import com.codeabovelab.dm.cluman.security.AccessContext;
import com.codeabovelab.dm.cluman.security.AccessContextFactory;
import com.codeabovelab.dm.cluman.security.SecuredType;
import com.codeabovelab.dm.cluman.security.TempAuth;
import com.codeabovelab.dm.cluman.ui.HttpException;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.kv.KeyValueStorage;
import com.codeabovelab.dm.common.kv.KvStorageEvent;
import com.codeabovelab.dm.common.kv.mapping.*;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.mb.Subscriptions;
import com.codeabovelab.dm.common.security.Action;
import com.codeabovelab.dm.common.utils.ExecutorUtils;
import com.codeabovelab.dm.common.validate.ValidityException;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Simple wrapper around InstanceStorage for save node info
 */
@ReConfigurable
@Component
public class NodeStorage implements NodeInfoProvider, NodeRegistry {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final KvMap<NodeRegistrationImpl> nodes;
    private final MessageBus<NodeEvent> nodeEventBus;
    private final MessageBus<DockerLogEvent> dockerLogBus;
    private final PersistentBusFactory persistentBusFactory;
    private final ExecutorService executorService;
    private final DockerEventsConfig dockerEventConfig;
    private final NodeStorageConfig config;
    private DockerServiceFactory dockerFactory;

    @Autowired
    public NodeStorage(NodeStorageConfig config,
                       KvMapperFactory kvmf,
                       @Qualifier(NodeEvent.BUS) MessageBus<NodeEvent> nodeEventBus,
                       @Qualifier(DockerServiceEvent.BUS) MessageBus<DockerServiceEvent> dockerBus,
                       @Qualifier(DockerLogEvent.BUS) MessageBus<DockerLogEvent> dockerLogBus,
                       DockerEventsConfig dockerEventConfig,
                       PersistentBusFactory persistentBusFactory) {
        this.config = config;
        this.nodeEventBus = nodeEventBus;
        this.persistentBusFactory = persistentBusFactory;
        this.dockerEventConfig = dockerEventConfig;
        this.dockerLogBus = dockerLogBus;
        KeyValueStorage storage = kvmf.getStorage();
        String nodesPrefix = storage.getPrefix() + "/nodes/";
        this.nodes = KvMap.builder(NodeRegistrationImpl.class, NodeInfoImpl.Builder.class)
          .path(nodesPrefix)
          .passDirty(true)
          .adapter(new NodesKvMapAdapterImpl(this))
          .localListener((e) -> {
              if(e.getAction() == KvMapLocalEvent.Action.CREATE) {
                  AccessContextFactory.getLocalContext().assertGranted(SecuredType.NODE.id(e.getKey()), Action.CREATE);
              }
          })
          .listener(this::onKVEvent)
          .mapper(kvmf)
          .build();
        log.info("{} initialized with config: {}", getClass().getSimpleName(), this.config);
        this.executorService = ExecutorUtils.executorBuilder()
          .name(getClass().getSimpleName())
          .maxSize(this.config.getMaxNodes())
          .rejectedHandler((runnable, executor) -> {
              String hint = "";
              try {
                  int nodes = this.nodes.list().size();
                  int maxNodes = this.config.getMaxNodes();
                  if(nodes > maxNodes) {
                      hint = "\nNote that 'config.maxNodes'=" + maxNodes + " but storage has 'nodes'=" + nodes;
                  }
              } catch (Exception e) {
                  //supress
              }
              throw new RejectedExecutionException("Task " + runnable + " rejected from " + executor + hint);
          })
          .build();
        dockerBus.subscribe(this::onDockerServiceEvent);
    }

    @Autowired
    void setDockerFactory(DockerServiceFactory dockerFactory) {
        this.dockerFactory = dockerFactory;
    }

    public Subscriptions<NodeEvent> getNodeEventSubscriptions() {
        return nodeEventBus.asSubscriptions();
    }

    @PostConstruct
    public void init() {
        nodes.load();
    }

    private void onKVEvent(KvMapEvent<NodeRegistrationImpl> e) {
        String key = e.getKey();
        KvStorageEvent.Crud action = e.getAction();
        try (TempAuth ta = TempAuth.asSystem()) {
            switch (action) {
                case DELETE: {
                    NodeRegistrationImpl nr = getNodeRegistrationInternal(key);
                    NodeInfoImpl ni = nr == null? NodeInfoImpl.builder().name(key).build() : nr.getNodeInfo();
                    fireNodeModification(nr, StandardActions.DELETE, ni, null);
                    break;
                }
                default: {
                    NodeRegistrationImpl nr = this.nodes.getIfPresent(key);
                    // update events will send from node registration
                    if (nr != null && action == KvStorageEvent.Crud.CREATE) {
                        fireNodeModification(nr, StandardActions.CREATE, null, nr.getNodeInfo());
                    }
                }
            }
        }
    }

    void fireNodeModification(NodeRegistrationImpl nr, String action, NodeInfoImpl old, NodeInfoImpl current) {
        if(old == null && current == null) {
            log.error("Something wrong:  old and current values of node '{}' is null, at action '{}'", nr.getName());
            return;
        }
        // NodeRegistrationImpl - may be null in some cases
        NodeEvent ne = NodeEvent.builder()
          .action(action)
          .current(current)
          .old(old)
          .build();
        //we use async execution only from another event handler
        this.executorService.execute(() -> {
            try (TempAuth auth = TempAuth.asSystem()) {
                this.nodeEventBus.accept(ne);
            }
        });
    }

    private void onDockerServiceEvent(DockerServiceEvent e) {
        try {
            if(e instanceof DockerServiceEvent.DockerServiceInfoEvent) {
                // we update health of all presented nodes
                // also it work only in standalone swarm cluster, and must be moved out here
                DockerServiceInfo info = ((DockerServiceEvent.DockerServiceInfoEvent) e).getInfo();
                for(NodeInfo node: info.getNodeList()) {
                    // we must not create nodes here,
                    // therefore they may be deleted before, and creation will restore its, that is not wanted
                    NodeRegistrationImpl reg = getNodeRegistrationInternal(node.getName());
                    if(reg != null) {
                        reg.updateHealth(node.getHealth());
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Can not update nodes.", ex);
        }
    }

    @Scheduled(fixedDelayString = SwarmUtils.EXPR_NODES_UPDATE_MS)
    private void checkNodes() {
        // periodically check online status of nodes
        try(TempAuth ta = TempAuth.asSystem()) {
            log.info("Begin update list of nodes");
            for(NodeRegistrationImpl nr: nodes.values()) {
                checkNode(nr);
            }
            log.info("End update list of nodes");
        }
    }

    private void checkNode(NodeRegistrationImpl nr) {
        log.info("Update node '{}' of '{}' cluster", nr.getName(), nr.getCluster());
        DockerServiceInfo tmp = null;
        try {
            tmp = nr.getDocker().getInfo();
        } catch (Exception e) {
            log.error("Fail to load node '{}' info due to error: {}", nr.getName(), e.toString());
        }
        final DockerServiceInfo dsi = tmp;
        nr.updateNodeInfo(b -> {
            NodeMetrics.Builder nmb = NodeMetrics.builder().from(b.getHealth());
            if(dsi != null) {
                b.setLabels(dsi.getLabels());
                nmb.setHealthy(true);
                nmb.setTime(dsi.getSystemTime());
                nmb.setSysMemTotal(dsi.getMemory());
                nmb.setState(NodeMetrics.State.HEALTHY);
            } else {
                nmb.setHealthy(false);
                nmb.setState(b.isOn()? NodeMetrics.State.UNHEALTHY : NodeMetrics.State.DISCONNECTED);
            }
            b.setHealth(nmb.build());
        });
        // this check offline status internal and cause status change event
        nr.getNodeInfo();
    }

    NodeRegistrationImpl newRegistration(NodeInfo nodeInfo) {
        NodeRegistrationImpl nr = new NodeRegistrationImpl(this, persistentBusFactory, nodeInfo);
        nr.setTtl(this.config.getUpdateSeconds() * 2);
        nr.init();
        return nr;
    }

    public boolean hasNode(Predicate<Object> predicate, String nodeId) {
        NodeRegistration instance = getNodeRegistrationInternal(nodeId);
        return instance != null && predicate.test(instance);
    }

    public NodeRegistration getNodeRegistration(String nodeId) {
        NodeRegistrationImpl nr = getNodeRegistrationInternal(nodeId);
        checkAccess(nr, Action.READ);
        return nr;
    }

    private void checkAccess(NodeRegistrationImpl nr, Action read) {
        if(nr == null) {
            return;
        }
        AccessContextFactory.getLocalContext().assertGranted(nr.getOid(), read);
    }

    /**
     * This method newer been covered by security, because used for security checks.
     * @param nodeId name of node
     * @return registration or null
     */
    NodeRegistrationImpl getNodeRegistrationInternal(String nodeId) {
        if(nodeId == null) {
            return null;
        }
        try {
            return nodes.get(nodeId);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if(cause instanceof ValidityException) {
                log.error("Not load node '{}' due validation error: {}", nodeId, cause.getMessage());
            } else {
                log.error("On loading: {} ", nodeId, e);
            }
        }
        return null;
    }

    public void removeNode(String name) {
        //we check name for prevent names like '../'
        NodeUtils.checkName(name);
        NodeRegistrationImpl nr = getNodeRegistrationInternal(name);
        checkAccess(nr, Action.DELETE);
        nodes.remove(name);
    }

    private NodeRegistrationImpl getOrCreateNodeRegistration(String name) {
        ExtendedAssert.matchAz09Hyp(name, "node name");
        return nodes.computeIfAbsent(name, (n) -> {
            AccessContextFactory.getLocalContext().assertGranted(SecuredType.NODE.id(name), Action.CREATE);
            NodeRegistrationImpl nr = newRegistration(NodeInfoImpl.builder().name(name));
            return nr;
        });
    }

    /**
     * Register or update node.
     * @param name name of node
     * @param ttl time while for node info is actual
     * @param updater handler which do node update
     */
    public NodeRegistration updateNode(String name, int ttl, Consumer<NodeInfoImpl.Builder> updater) {
        NodeRegistrationImpl nr = getOrCreateNodeRegistration(name);
        nr.setTtl(ttl);// important that it must be before other update methods
        nr.updateNodeInfo(updater);
        save(nr);
        return nr;
    }

    private void save(NodeRegistrationImpl nr) {
        Assert.notNull(nr, "NodeRegistrationImpl is null");
        // we use copy of node info, for data consistency
        nodes.flush(nr.getName());
    }

    public void setNodeCluster(String nodeName, String cluster) {
        NodeRegistrationImpl nr = getNodeRegistrationInternal(nodeName);
        if(nr == null) {
            throw new HttpException(HttpStatus.NOT_FOUND, "Node '" + nodeName + "' is not found");
        }
        checkAccess(nr, Action.UPDATE);
        NodeInfo ni = nr.getNodeInfo();
        String oldCluster = ni.getCluster();
        if(!Objects.equals(oldCluster, cluster)) {
            //here may be race condition
            nr.setCluster(cluster);
            save(nr);
        }
    }

    @Override
    public String getNodeCluster(String node) {
        NodeRegistrationImpl nr = getNodeRegistrationInternal(node);
        if (nr == null) {
            return null;
        }
        return nr.getCluster();
    }

    @Override
    public NodeInfo getNodeInfo(String node) {
        NodeRegistrationImpl instance = getNodeRegistrationInternal(node);
        if(instance == null) {
            return null;
        }
        checkAccess(instance, Action.READ);
        return instance.getNodeInfo();
    }

    /**
     *
     * @param predicate functor which is return true for InstanceInfo's which will be passed to result.
     * @return
     */
    public List<NodeInfo> getNodes(Predicate<? super NodeRegistration> predicate) {
        Set<String> keys = listNodeNames();
        AccessContext ac = AccessContextFactory.getLocalContext();
        List<NodeInfo> nodeList = new ArrayList<>(keys.size());
        for (String key : keys) {
            NodeRegistrationImpl nr = getNodeRegistrationInternal(key);
            // when node invalid we may receive null
            if (nr == null || !predicate.test(nr) || !ac.isGranted(nr.getOid(), Action.READ)) {
                continue;
            }
            nodeList.add(nr.getNodeInfo());
        }
        nodeList.sort(null);
        return nodeList;
    }

    public Collection<String> getNodeNames() {
        return ImmutableSet.copyOf(nodes.list());
    }

    private Set<String> listNodeNames() {
        return nodes.list();
    }

    @ReConfigObject
    private NodeStorageConfigObj getConfig() {
        Set<String> nodeNames = listNodeNames();
        NodeStorageConfigObj obj = new NodeStorageConfigObj();
        List<NodeInfoImpl> list = new ArrayList<>();
        obj.setNodes(list);
        for(String nodeName: nodeNames) {
            NodeRegistrationImpl nri = getNodeRegistrationInternal(nodeName);
            if(nri == null) {
                continue;
            }
            list.add(nri.getNodeInfo());
        }
        return obj;
    }

    @ReConfigObject
    private void setConfig(NodeStorageConfigObj o) {
        List<NodeInfoImpl> nodes = o.getNodes();
        for(NodeInfoImpl nc: nodes) {
            NodeRegistrationImpl nr = getOrCreateNodeRegistration(nc.getName());
            if(nr == null) {
                return;
            }
            nr.updateNodeInfo(b -> {
                NodeMetrics om = b.getHealth();
                b.from(nc);
                b.health(om);
            });
            save(nr);
        }
    }

    public DockerService getNodeService(String nodeName) {
        NodeRegistrationImpl nr = nodes.get(nodeName);
        if(nr == null) {
            return null;
        }
        DockerService service = nr.getDocker();
        return service;
    }

    public DockerService registerNode(String nodeName, String address) {
        NodeRegistrationImpl nr = getOrCreateNodeRegistration(nodeName);
        String oldAddr = nr.getAddress();
        DockerService ds = nr.setAddress(address);
        save(nr);
        if(!Objects.equals(oldAddr, address)) {
            // address changed, force to update node status
            checkNode(nr);
        }
        return ds;
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


    DockerService createNodeService(NodeRegistrationImpl nr) {
        // we intentionally register node without specifying cluster
        ClusterConfig config = configForNode(nr.getAddress()).build();
        return dockerFactory.createDockerService(config, (b) -> b.setNode(nr.getName()));
    }

    void acceptDockerLogEvent(DockerLogEvent logEvent) {
        this.executorService.execute(() -> {
            try (TempAuth auth = TempAuth.asSystem()) {
                this.dockerLogBus.accept(logEvent);
            }
        });
    }

    DockerEventsConfig getDockerEventConfig() {
        return dockerEventConfig;
    }

    NodeStorageConfig getStorageConfig() {
        return config;
    }
}
