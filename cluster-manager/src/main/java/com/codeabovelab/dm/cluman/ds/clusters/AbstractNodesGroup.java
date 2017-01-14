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

import com.codeabovelab.dm.cluman.model.NodeGroupState;
import com.codeabovelab.dm.cluman.security.AclModifier;
import com.codeabovelab.dm.cluman.security.SecuredType;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.common.security.SecurityUtils;
import com.codeabovelab.dm.common.security.TenantPrincipalSid;
import com.codeabovelab.dm.common.security.acl.AclSource;
import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import com.google.common.collect.ImmutableSet;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 */
@ToString
public abstract class AbstractNodesGroup<C extends AbstractNodesGroupConfig<C>> implements NodesGroup, AutoCloseable {

    protected static final int S_BEGIN = 0;
    protected static final int S_INITING = 1;
    protected static final int S_INITED = 2;
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
    protected final AtomicInteger state = new AtomicInteger(S_BEGIN);

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
    }

    /**
     * Try to init cluster if it not inited already.
     * @see #getState()
     */
    public final void init() {
        if(!state.compareAndSet(S_BEGIN, S_INITING)) {
            return;
        }
        try {
            log.info("Begin init of cluster '{}'", getName());
            initImpl();
            if(state.compareAndSet(S_INITING, S_INITED)) {
                log.info("Success init of cluster '{}'", getName());
            }
        } finally {
            if(state.compareAndSet(S_INITING, S_FAILED)) {
                log.error("Fail to init of cluster '{}'", getName());
            }
        }
    }

    protected void initImpl() {
        //none
    }

    @Override
    public void close() {
        closeImpl();
    }

    protected void closeImpl() {
        //none
    }

    @Override
    public NodeGroupState getState() {
        NodeGroupState.Builder b = NodeGroupState.builder();
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
        }
        flush();
    }

    @Override
    public void updateConfig(Consumer<AbstractNodesGroupConfig<?>> consumer) {
        synchronized (lock) {
            C clone = (C) config.clone();
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
}
