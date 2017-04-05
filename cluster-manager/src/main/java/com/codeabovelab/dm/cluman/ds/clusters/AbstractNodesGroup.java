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
import com.codeabovelab.dm.cluman.cluster.docker.model.Network;
import com.codeabovelab.dm.cluman.ds.swarm.NetworkManager;
import com.codeabovelab.dm.cluman.model.NodeGroupState;
import com.codeabovelab.dm.cluman.security.AclModifier;
import com.codeabovelab.dm.cluman.security.SecuredType;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.cluman.security.TempAuth;
import com.codeabovelab.dm.common.security.SecurityUtils;
import com.codeabovelab.dm.common.security.TenantPrincipalSid;
import com.codeabovelab.dm.common.security.acl.AclSource;
import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import com.google.common.collect.ImmutableSet;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 */
@ToString
public abstract class AbstractNodesGroup<C extends AbstractNodesGroupConfig<C>> implements NodesGroup, AutoCloseable {

    protected static final int S_BEGIN = 0;
    protected static final int S_INITING = 1;
    protected static final int S_INITED = 2;
    protected static final int S_CLEANING = 3;
    protected static final int S_CLEANED = 4;
    protected static final int S_FAILED = 99;
    // not use static or @Slf4j annotation in this case
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Class<C> configClazz;
    private final Set<Feature> features;
    private final DiscoveryStorageImpl storage;
    private final String name;
    protected volatile C config;
    protected final Object lock = new Object();
    private final ObjectIdentityData oid;
    private final AtomicInteger state = new AtomicInteger(S_BEGIN);
    private volatile String stateMessage;
    protected final NetworkManager networkManager;
    private final CreateNetworkTask createNetworkTask = new CreateNetworkTask();

    @SuppressWarnings("unchecked")
    public AbstractNodesGroup(C config,
                              DiscoveryStorageImpl storage,
                              Collection<Feature> features) {
        this.storage = storage;
        Assert.notNull(storage, "storage is null");
        this.configClazz = (Class<C>) config.getClass();
        String name = config.getName();
        Assert.notNull(name, "name is null");
        this.name = name;
        this.oid = SecuredType.CLUSTER.id(name);
        this.features = features == null? Collections.emptySet() : ImmutableSet.copyOf(features);
        setConfig(config.clone());
        this.networkManager = new NetworkManager(this);
    }

    /**
     * Try to init cluster if it not inited already.
     * @see #getState()
     */
    public final void init() {
        if(!changeState(S_BEGIN, S_INITING)) {
            return;
        }
        try {
            log.info("Begin init of cluster '{}'", getName());
            initImpl();
            if(changeState(S_INITING, S_INITED)) {
                log.info("Success init of cluster '{}'", getName());
            }
        } finally {
            if(changeState(S_INITING, S_FAILED)) {
                // NOTE: if cluster may be reinited then initImpl() MUST:
                // - properly handle any errors
                // - set status to S_BEGIN after errors which prevent correct initialisation (like node is offline)
                //otherwise cluster will gone to failed state, which is unrecoverable
                log.error("Fail to init of cluster '{}'", getName());
            }
        }
    }

    protected void initImpl() {
        //none
    }

    protected final void cancelInit(String msg) {
        log.warn("Init of {} cluster cancelled due to: {}", getName(), msg);
        changeState(S_INITING, S_BEGIN, msg);
    }

    @Override
    public final void close() {
        closeImpl();
    }

    protected void closeImpl() {
        //none
    }

    @Override
    public final void clean() {
        if(changeState(S_INITED, S_CLEANING)) {
            try {
                cleanImpl();
            } finally {
                changeState(S_CLEANING, S_CLEANED);
            }
        }
    }

    private boolean changeState(int oldState, int newState) {
        return changeState(oldState, newState, null);
    }

    private boolean changeState(int oldState, int newState, String msg) {
        boolean res = state.compareAndSet(oldState, newState);
        if(res) {
            this.stateMessage = msg;
        }
        return res;
    }

    protected int getStateCode() {
        return state.get();
    }

    protected void cleanImpl() {
        // nothing, override it for clean cluster
    }

    @Override
    public NodeGroupState getState() {
        NodeGroupState.Builder b = NodeGroupState.builder();
        String msg = this.stateMessage;
        b.inited(true);
        switch (state.get()) {
            case S_BEGIN:
            case S_INITING:
                b.message("Not inited.");
                b.ok(false);
                b.inited(false);
                break;
            case S_FAILED:
                b.message("Failed.");
                b.ok(false);
                break;
            case S_INITED:
                b.ok(true);
                break;
            default:
                b.ok(false);
                b.message("Unknown state.");
        }
        if(msg != null) {
            b.message(msg);
        }
        return b.build();
    }

    @Override
    public void flush() {
        storage.getKvMap().flush(name);
    }

    public String getImageFilter() {
        synchronized (lock) {
            return config.getImageFilter();
        }
    }

    public void setImageFilter(String imageFilter) {
        synchronized (lock) {
            onSet("imageFilter", this.config.getImageFilter(), imageFilter);
            this.config.setImageFilter(imageFilter);
        }
    }

    protected void onSet(String name, Object oldVal, Object newVal) {
        synchronized (lock) {
            //TODO remove after test mapper.onSet(name, oldVal, newVal);
        }
    }

    @Override
    public String getTitle() {
        synchronized (lock) {
            return config.getTitle();
        }
    }

    @Override
    public void setTitle(String title) {
        synchronized (lock) {
            onSet("title", this.config.getTitle(), title);
            this.config.setTitle(title);
        }
    }

    @Override
    public String getDescription() {
        synchronized (lock) {
            return config.getDescription();
        }
    }

    @Override
    public void setDescription(String description) {
        synchronized (lock) {
            onSet("description", this.config.getDescription(), description);
            this.config.setDescription(description);
        }
    }

    protected NodeStorage getNodeStorage() {
        return storage.getNodeStorage();
    }

    DiscoveryStorageImpl getDiscoveryStorage() {
        return storage;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<Feature> getFeatures() {
        return this.features;
    }

    /**
     * Note that this method may change config
     * @see AbstractNodesGroupConfig#getDefaultNetwork()
     * @return non null name of default network
     */
    @Override
    public String getDefaultNetworkName() {
        String defaultNetwork;
        synchronized (lock) {
            defaultNetwork = this.config.getDefaultNetwork();
            if(defaultNetwork == null) {
                defaultNetwork = getName();
            }
        }
        return defaultNetwork;
    }

    protected void createDefaultNetwork() {
        NodeGroupState state = getState();
        if (!state.isOk()) {
            log.warn("Can not create network due cluster '{}' in '{}' state.", getName(), state.getMessage());
            return;
        }
        getDiscoveryStorage().getExecutor().execute(createNetworkTask);
    }

    @Override
    public NetworkManager getNetworks() {
        return networkManager;
    }

    @Override
    public ObjectIdentityData getOid() {
        return oid;
    }

    @Override
    public AclSource getAcl() {
        synchronized (lock) {
            AclSource acl = this.config.getAcl();
            if(acl == null) {
                // we must not return null, but also can not update config here, so make default non null value
                acl = defaultAclBuilder().build();
            }
            return acl;
        }
    }

    private AclSource.Builder defaultAclBuilder() {
        return AclSource.builder()
          .owner(TenantPrincipalSid.from(SecurityUtils.USER_SYSTEM))
          .objectIdentity(oid);
    }

    @Override
    public void updateAcl(AclModifier operator) {
        synchronized (lock) {
            AclSource acl = this.config.getAcl();
            AclSource.Builder b = defaultAclBuilder().from(acl);
            if(!operator.modify(b)) {
                return;
            }
            // we set true oid before modification for using in modifier, but not allow modifier to change it
            if(!oid.equals(b.getObjectIdentity())) {
                throw new IllegalArgumentException("Invalid oid of updated acl: " + b);
            }
            AclSource modified = b.build();
            onSet("acl", acl, modified);
            this.config.setAcl(modified);
        }
        flush();
    }

    @Override
    public C getConfig() {
        synchronized (lock) {
            return config.clone();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setConfig(AbstractNodesGroupConfig<?> config) {
        this.configClazz.cast(config);
        validateConfig(config);
        synchronized (lock) {
            this.config = (C) config.clone();
            onConfig();
        }
        flush();
    }

    /**
     * here you can handle config update, before flush
     */
    protected void onConfig() {
    }

    @Override
    public void updateConfig(Consumer<AbstractNodesGroupConfig<?>> consumer) {
        synchronized (lock) {
            C clone = config.clone();
            consumer.accept(clone);
            validateConfig(clone);
            this.config = clone;
        }
        flush();
    }

    private void validateConfig(AbstractNodesGroupConfig<?> config) {
        AclSource acl = config.getAcl();
        if(acl != null && !oid.equals(acl.getObjectIdentity())) {
            throw new IllegalArgumentException("Bad acl.objectIdentity in config: " + config);
        }
    }

    private class CreateNetworkTask implements Runnable {

        private final Lock lock = new ReentrantLock();

        @Override
        public void run() {
            if(!lock.tryLock()) {
                // this case actual when one of tasks already in execution
                return;
            }
            try (TempAuth ta = TempAuth.asSystem()) {
                DockerService docker = getDocker();
                if(docker == null || !docker.isOnline()) {
                    log.warn("Can not create networks in '{}' cluster due to null or offline docker", getName());
                    return;
                }
                List<Network> networks = docker.getNetworks();
                log.debug("Networks {}", networks);
                String defaultNetwork = getDefaultNetworkName();
                Optional<Network> any = networks.stream().filter(n -> n.getName().equals(defaultNetwork)).findAny();
                if (any.isPresent()) {
                    return;
                }
                networkManager.createNetwork(defaultNetwork);
            } finally {
                lock.unlock();
            }
        }
    }
}
