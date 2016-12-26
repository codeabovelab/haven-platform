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

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.filter.FilterFactory;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.reconfig.ReConfigObject;
import com.codeabovelab.dm.cluman.reconfig.ReConfigurable;
import com.codeabovelab.dm.cluman.security.*;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.kv.KeyValueStorage;
import com.codeabovelab.dm.common.kv.KvStorageEvent;
import com.codeabovelab.dm.common.kv.mapping.KvMap;
import com.codeabovelab.dm.common.kv.mapping.KvMapAdapter;
import com.codeabovelab.dm.common.kv.mapping.KvMapLocalEvent;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.security.Action;
import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.TenantGrantedAuthoritySid;
import com.codeabovelab.dm.common.security.acl.AceSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * swarm discovery storage implementation based on eureka
 */
@ReConfigurable
@Primary
@Component
public class DiscoveryStorageImpl implements DiscoveryStorage {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DockerServices services;
    private final NodeStorage nodeStorage;
    private final KvMap<NodesGroup> clusters;
    private final KvMapperFactory kvmf;
    private final String prefix;
    private final FilterFactory filterFactory;
    private final AccessContextFactory aclContextFactory;
    private final MessageBus<NodesGroupEvent> messageBus;

    @Autowired
    public DiscoveryStorageImpl(KvMapperFactory kvmf,
                                FilterFactory filterFactory,
                                DockerServices dockerServices,
                                NodeStorage nodeStorage,
                                AccessContextFactory aclContextFactory,
                                @Qualifier(NodesGroupEvent.BUS) MessageBus<NodesGroupEvent> messageBus) {
        this.kvmf = kvmf;
        this.services = dockerServices;
        this.nodeStorage = nodeStorage;
        this.messageBus = messageBus;
        this.aclContextFactory = aclContextFactory;
        KeyValueStorage storage = kvmf.getStorage();
        this.filterFactory = filterFactory;
        this.prefix = storage.getPrefix() + "/clusters/";
        this.clusters = KvMap.builder(NodesGroup.class, AbstractNodesGroupConfig.class)
          .path(prefix)
          .mapper(kvmf)
          .adapter(new KvMapAdapterImpl())
          .listener(e -> {
              String key = e.getKey();
              switch (e.getAction()) {
                  case DELETE:
                      fireGroupEvent(key, StandardActions.DELETE);
                      break;
                  case CREATE:
                      fireGroupEvent(key, StandardActions.CREATE);
                      break;
                  case UPDATE:
                      fireGroupEvent(key, StandardActions.UPDATE);
              }
          })
          .localListener(e -> {
              if(e.getAction() == KvMapLocalEvent.Action.CREATE) {
                  checkThatCanCreate();
              }
          })
          .build();

        filterFactory.registerFilter(new OrphansNodeFilterFactory(this));
        try(TempAuth ta = TempAuth.asSystem()) {
            AclModifier aclModifier = (asb) -> {
                //here we add default rights to read this groups by all users
                asb.addEntry(AceSource.builder()
                  .permission(Action.READ)
                  .granting(true)
                  .sid(TenantGrantedAuthoritySid.from(Authorities.USER))
                  .build());
                return true;
            };
            // virtual cluster for any nodes
            getOrCreateGroup(new DefaultNodesGroupConfig(GROUP_ID_ALL, FilterFactory.ANY)).updateAcl(aclModifier);
            // virtual cluster for nodes without cluster
            getOrCreateGroup(new DefaultNodesGroupConfig(GROUP_ID_ORPHANS, OrphansNodeFilterFactory.FILTER)).updateAcl(aclModifier);
        }
    }

    public void load() {
        try(TempAuth ta = TempAuth.asSystem()) {
            clusters.load();
        } catch (Exception  e) {
            log.error("Can not load clusters from storage", e);
        }
    }

    String getPrefix() {
        return prefix;
    }

    KvMap<NodesGroup> getKvMap() {
        return clusters;
    }

    DockerServices getDockerServices() {
        return this.services;
    }

    public NodeStorage getNodeStorage() {
        return nodeStorage;
    }

    @Override
    public NodesGroup getClusterForNode(String nodeId, String clusterId) {
        NodesGroup cluster = findNodeCluster(nodeId);
        if (cluster == null && clusterId != null) {
            cluster = getOrCreateCluster(clusterId, null);
        }
        if(cluster == null) {
            // no clusters for node, so it orphan
            cluster = clusters.get(GROUP_ID_ORPHANS);
        }
        checkThatCanRead(cluster);
        return cluster;
    }

    /**
     * get or create cluster. Consumer will be invoked before cluster process start and allow modification of swarm parameters
     * @param clusterId
     * @param consumer consumer or null
     * @return
     */
    @Override
    public NodesGroup getOrCreateCluster(String clusterId, Consumer<ClusterCreationContext> consumer) {
        ExtendedAssert.matchAz09Hyp(clusterId, "clusterId");
        NodesGroup ng = clusters.computeIfAbsent(clusterId, (cid) -> {
            checkThatCanCreate();
            return RealCluster.factory(this, null, consumer).apply(cid);
        });
        // we place it after creation, because check it for not created cluster is not good
        checkThatCanRead(ng);
        return ng;
    }

    private void checkThatCanCreate() {
        aclContextFactory.getContext().assertGranted(SecuredType.CLUSTER.typeId(), Action.CREATE);
    }

    private void checkThatCanRead(NodesGroup ng) {
        if(ng == null) {
            return;
        }
        aclContextFactory.getContext().assertGranted(SecuredType.CLUSTER.id(ng.getName()), Action.READ);
    }

    @Override
    public NodesGroup getOrCreateGroup(AbstractNodesGroupConfig<?> config) {
        final String clusterId = config.getName();
        ExtendedAssert.matchAz09Hyp(clusterId, "clusterId");
        NodesGroup ng = clusters.computeIfAbsent(clusterId, (cid) -> {
            checkThatCanCreate();
            return makeClusterFromConfig(config, cid);
        });
        // we place it after creation, because check it for not created cluster is not good
        checkThatCanRead(ng);
        return ng;
    }

    private NodesGroup makeClusterFromConfig(AbstractNodesGroupConfig<?> config, String cid) {
        NodesGroup cluster;
        if (config instanceof DefaultNodesGroupConfig) {
            cluster = NodesGroupImpl.builder()
              .config((DefaultNodesGroupConfig) config)
              .filterFactory(filterFactory)
              .storage(this)
              .dockerServices(services)
              .feature(NodesGroup.Feature.FORBID_NODE_ADDITION)
              .build();
        } else if (config instanceof SwarmNodesGroupConfig) {
            cluster = RealCluster.factory(this, (SwarmNodesGroupConfig) config, null).apply(cid);
        } else {
            throw new IllegalArgumentException("Unsupported config: " + config);
        }
        return cluster;
    }

    @Override
    public NodesGroup getClusterForNode(String nodeId) {
        NodesGroup cluster = findNodeCluster(nodeId);
        checkThatCanRead(cluster);
        return cluster;
    }


    private NodesGroup findNodeCluster(String nodeId) {
        //we need resolve real cluster or orphans otherwise
        NodesGroup candidate = clusters.get(GROUP_ID_ORPHANS);
        for (NodesGroup cluster : clusters.values()) {
            if ((candidate == null || (isVirtual(candidate) && !isVirtual(cluster))) &&
                  cluster.hasNode(nodeId)) {
                candidate = cluster;
            }
        }
        return candidate;
    }

    private boolean isVirtual(NodesGroup cluster) {
        return cluster instanceof NodesGroupImpl;
    }

    /**
     * Need only for {@link ClusterAclProvider } - it prevent recursion on security checks.
     * @param clusterId
     * @return
     */
    NodesGroup getClusterBypass(String clusterId) {
        NodesGroup ng = clusters.get(clusterId);
        return ng;
    }

    @Override
    public NodesGroup getCluster(String clusterId) {
        NodesGroup ng = clusters.get(clusterId);
        checkThatCanRead(ng);
        return ng;
    }

    @Override
    public void deleteCluster(String clusterId) {
        NodesGroup cluster = clusters.get(clusterId);
        ExtendedAssert.notFound(cluster, "Cluster: " + clusterId + " is not found.");
        Assert.isTrue(cluster instanceof RealCluster, "Can not delete non real cluster: " + clusterId);
        deleteGroup(clusterId);
    }

    private void deleteGroup(String clusterId) {
        aclContextFactory.getContext().assertGranted(SecuredType.CLUSTER.id(clusterId), Action.DELETE);
        clusters.remove(clusterId);
        log.error("Delete '{}' cluster.", clusterId);
    }

    public void deleteNodeGroup(String clusterId) {

        NodesGroup cluster = clusters.get(clusterId);
        Assert.notNull(cluster, "GroupId: " + clusterId + " is not found.");
        Assert.isTrue(!(cluster instanceof RealCluster), "Can not delete a real cluster: " + clusterId);
        Assert.isTrue(!SYSTEM_GROUPS.contains(clusterId), "Can't delete system group");
        deleteGroup(clusterId);
    }

    @Override
    public DockerService getService(String clusterId) {
        Assert.hasText(clusterId, "Name of cluster is null or empty");
        NodesGroup eurekaCluster = clusters.get(clusterId);
        ExtendedAssert.notFound(eurekaCluster, "Registry does not contains service with clusterId: " + clusterId);
        return eurekaCluster.getDocker();
    }

    @Override
    public Set<String> getServices() {
        ImmutableSet.Builder<String> isb = ImmutableSet.builder();
        AccessContext ac = aclContextFactory.getContext();
        this.clusters.forEach((k, v) -> {
            if(ac.isGranted(v.getOid(), Action.READ)) {
                isb.add(k);
            }
        });
        return isb.build();
    }

    private void fireGroupEvent(String clusterId, String action) {
        NodesGroupEvent.Builder logEvent = new NodesGroupEvent.Builder();
        logEvent.setAction(action);
        logEvent.setCluster(clusterId);
        logEvent.setSeverity(StandardActions.toSeverity(action));
        fireGroupEvent(logEvent);
    }

    private void fireGroupEvent(NodesGroupEvent.Builder eventBuilder) {
        messageBus.accept(eventBuilder.build());
    }

    /**
     * It need only for {@link ClusterAclProvider }
     * @param consumer
     */
    void getClustersBypass(Consumer<NodesGroup> consumer) {
        this.clusters.forEach((k, v) -> {
            consumer.accept(v);
        });
    }

    @Override
    public List<NodesGroup> getClusters() {
        ImmutableList.Builder<NodesGroup> ilb = ImmutableList.builder();
        AccessContext ac = aclContextFactory.getContext();
        this.clusters.forEach((k, v) -> {
            if(ac.isGranted(v.getOid(), Action.READ)) {
                ilb.add(v);
            }
        });
        return ilb.build();
    }

    @ReConfigObject
    private NodesGroupsConfig getConfig() {
        List<AbstractNodesGroupConfig<?>> groups = clusters.values()
          .stream()
          .map((c) -> c.getConfig())
          .collect(Collectors.toList());
        NodesGroupsConfig ngc = new NodesGroupsConfig();
        ngc.setGroups(groups);
        return ngc;
    }

    @ReConfigObject
    private void setConfig(NodesGroupsConfig config) {
        List<AbstractNodesGroupConfig<?>> groupsConfigs = config.getGroups();
        for(AbstractNodesGroupConfig<?> groupConfig: groupsConfigs) {
            NodesGroup group = getOrCreateGroup(groupConfig);
            if(group != null) {
                group.flush();
            }
        }
    }

    private class KvMapAdapterImpl implements KvMapAdapter<NodesGroup> {
        @Override
        public Object get(String key, NodesGroup src) {
            return src.getConfig();
        }

        @Override
        public NodesGroup set(String key, NodesGroup src, Object value) {
            AbstractNodesGroupConfig<?> config = (AbstractNodesGroupConfig<?>) value;
            if(src != null) {
                src.setConfig(config);
            } else {
                src = makeClusterFromConfig(config, key);
            }
            return src;
        }

        @Override
        public Class<?> getType(NodesGroup src) {
            if(src == null) {
                return null;
            }
            AbstractNodesGroupConfig<?> config = src.getConfig();
            if(config != null) {
                return config.getClass();
            }
            //src.getClass().getTypeParameters();
            return null;
        }
    }
}
