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

import com.codeabovelab.dm.common.kv.mapping.KvMapper;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.common.security.acl.AclSource;
import com.google.common.collect.ImmutableSet;
import lombok.ToString;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.UnaryOperator;

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
        this.features = features == null? Collections.emptySet() : ImmutableSet.copyOf(features);
        setConfig(config.clone());
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
            return this.config.getAcl();
        }
    }

    @Override
    public void updateAcl(UnaryOperator<AclSource> operator) {
        KvMapper<C> mapper;
        synchronized (lock) {
            AclSource acl = this.config.getAcl();
            AclSource modified = operator.apply(acl);
            if(modified == acl) {
                return;
            }
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
        KvMapper<C> mapper;
        synchronized (lock) {
            this.config = (C) config.clone();
            mapper = this.mapper = storage.getKvMapperFactory().createMapper(this.config, storage.getPrefix() + name);
        }
        mapper.save();
    }
}
