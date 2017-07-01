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

package com.codeabovelab.dm.common.kv.mapping;

import com.codeabovelab.dm.common.kv.*;
import com.codeabovelab.dm.common.validate.Validity;
import com.codeabovelab.dm.common.validate.ValidityException;
import com.google.common.base.MoreObjects;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

/**
 */
@Slf4j
public class KvClassMapper<T> {

    private static final KvObjectFactory<Object> FACTORY = (String key, Class<?> type) -> BeanUtils.instantiate(type);

    @Data
    public static class Builder<T> {
        private final KvMapperFactory mapperFactory;
        private final Class<T> type;
        private String prefix;
        private KvObjectFactory<T> factory;

        Builder(KvMapperFactory mapperFactory, Class<T> type) {
            this.mapperFactory = mapperFactory;
            this.type = type;
        }

        public Builder<T> prefix(String prefix) {
            setPrefix(prefix);
            return this;
        }

        public Builder<T> factory(KvObjectFactory<T> factory) {
            setFactory(factory);
            return this;
        }

        public KvClassMapper<T> build() {
            return new KvClassMapper<>(this);
        }
    }

    private final Class<T> type;
    private final String prefix;
    private final KvMapperFactory mapper;
    private final KeyValueStorage storage;
    private final AbstractMapping<T> mapping;

    @SuppressWarnings("unchecked")
    KvClassMapper(Builder<T> builder) {
        this.mapper = builder.mapperFactory;
        this.prefix = builder.prefix;
        this.type = builder.type;
        this.mapping = this.mapper.getMapping(type, MoreObjects.firstNonNull(builder.factory, (KvObjectFactory<T>) FACTORY));
        this.storage = mapper.getStorage();
    }

    public static <T> Builder<T> builder(KvMapperFactory mf, Class<T> type) {
        return new Builder<>(mf, type);
    }

    public Class<T> getType() {
        return type;
    }

    public String getPrefix() {
        return prefix;
    }

    public void delete(String name) {
        this.storage.deletedir(path(name), DeleteDirOptions.builder().recursive(true).build());
    }

    private String path(String name) {
        return KvUtils.join(this.prefix, name);
    }

    /**
     * Save object to storage and return object that allow check for object modifications.
     * @param name
     * @param object
     * @return
     */
    public void save(String name, T object) {
        save(name, object, null);
    }

    void save(String name, T object, KvSaveCallback callback) {
        this.type.cast(object);
        String path = path(name);
        this.mapping.save(path, object, callback);
    }

    /**
     * list same objects in storage
     * @return
     */
    public List<String> list() {
        List<String> list = this.storage.list(prefix);
        if (list == null) {
            createPrefix();
            list = this.storage.list(prefix);
        }
        return list.stream().map(this::getName).collect(Collectors.toList());
    }

    /**
     * Get relative name from full path.
     * @param path
     * @return
     */
    public String getName(String path) {
        return KvUtils.name(prefix, path);
    }

    private void createPrefix() {
        this.storage.setdir(prefix, WriteOptions.builder().failIfExists(true).build());
    }

    /**
     * Load object from specified node. Name of node can be obtained from {@link #list()}.
     * @param name name of node
     * @return object or null
     */
    public T load(String name) {
        return load(name, (Class<T>)null);
    }

    /**
     * Load object from specified node. Name of node can be obtained from {@link #list()}.
     * @param name name of node
     * @param type null or instantiable type, must be a subtype of {@link T}
     * @return object or null
     */
    public <S extends T> S load(String name, Class<S> type) {
        String path = path(name);
        //check that mapped dir is exists
        KvNode node = this.storage.get(path);
        if(node == null) {
            return null;
        }
        Class<S> actualType = resolveType(type);
        return this.mapping.load(path, name, actualType);
    }

    @SuppressWarnings("unchecked")
    private <S extends T> Class<S> resolveType(Class<S> subType) {
        Class<S> actualType = (Class<S>) this.type;
        if(subType != null) {
            Assert.isTrue(this.type.isAssignableFrom(subType), "Specified type " + subType + " must be an subtype of " + this.type);
            actualType = subType;
        }
        return actualType;
    }

    /**
     * Load into existed object
     * @param name
     * @param object
     */
    public void load(String name, T object) {
        String path = path(name);
        this.mapping.load(path, object);
        Validity validity = mapper.validate(path, object);
        if(!validity.isValid()) {
            throw new ValidityException("Invalid : ", validity);
        }
    }
}
