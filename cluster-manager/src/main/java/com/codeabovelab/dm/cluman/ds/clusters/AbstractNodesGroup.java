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

import com.codeabovelab.dm.cluman.security.AclModifier;
import com.codeabovelab.dm.cluman.security.SecuredType;
import com.codeabovelab.dm.common.kv.mapping.KvMapper;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.common.security.SecurityUtils;
import com.codeabovelab.dm.common.security.TenantPrincipalSid;
import com.codeabovelab.dm.common.security.acl.AclSource;
import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import com.google.common.collect.ImmutableSet;
import lombok.ToString;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 */
@ToString
abstract class AbstractNodesGroup<T extends AbstractNodesGroup<T, C>, C extends AbstractNodesGroupConfig<C>> implements NodesGroup {

    private final Class<C> configClazz;
    private final Set<Feature> features;
    private final DiscoveryStorageImpl storage;
    private final String name;
    private volatile KvMapper<C> mapper;
    protected volatile C config;
    protected final Object lock = new Object();
    private final ObjectIdentityData oid;

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

    protected void init() {
        //none
    }

    @Override
    public void flush() {
        KvMapper<C> mapper;
        synchronized (lock) {
            mapper = this.mapper;
        }
        mapper.save();
    }

    KvMapper<C> getMapper() {
        synchronized (lock) {
            return mapper;
        }
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
            mapper.onSet(name, oldVal, newVal);
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
        KvMapper<C> mapper;
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
            mapper = this.mapper;
        }
        mapper.save();
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
        KvMapper<C> mapper;
        synchronized (lock) {
            this.config = (C) config.clone();
            mapper = this.mapper = storage.getKvMapperFactory().createMapper(this.config, storage.getPrefix() + name);
        }
        mapper.save();
    }

    private void validateConfig(AbstractNodesGroupConfig<?> config) {
        AclSource acl = config.getAcl();
        if(acl != null && !oid.equals(acl.getObjectIdentity())) {
            throw new IllegalArgumentException("Bad acl.objectIdentity in config: " + config);
        }
    }
}
