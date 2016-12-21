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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.MoreObjects;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 */
@Slf4j
public class KvClassMapper<T> {

    private static final KvObjectFactory<Object> FACTORY = (String key, Class<?> type) -> {
        return BeanUtils.instantiate(type);
    };

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

    private static final String PROP_TYPE = "@class";
    private final Class<T> type;
    private final String prefix;
    private final KvMapperFactory mapper;
    private final Map<Class, Map<String, KvProperty>> props = new HashMap<>();
    private final KeyValueStorage storage;
    private final KvObjectFactory<T> factory;


    @SuppressWarnings("unchecked")
    KvClassMapper(Builder<T> builder) {
        this.mapper = builder.mapperFactory;
        this.prefix = builder.prefix;
        this.type = builder.type;
        this.factory = MoreObjects.firstNonNull(builder.factory, (KvObjectFactory<T>) FACTORY);
        Map<String, KvProperty> map = this.mapper.loadProps(type, p -> p);
        this.props.put(type, map);
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

    void save(String name, T object, KvPropertySetCallback callback) {
        this.type.cast(object);
        String path = path(name);
        // we must read index of all node but, we can not create node in single command,
        // so we use index of last sub node
        Collection<KvProperty> props = getProps(object);
        if(props.isEmpty()) {
            throw new IllegalArgumentException("The key '" + name +
              "' is mapped to object of type " + object.getClass() + " which has no properties.");
        }
        //store type of object
        this.storage.set(KvUtils.join(path, PROP_TYPE), object.getClass().getName());
        //store properties
        for(KvProperty property: props) {
            String strval = property.get(object);
            String proppath = KvUtils.join(path, property.getKey());
            try {
                KvNode res = this.storage.set(proppath, strval);
                if(callback != null) {
                    callback.call(property, res);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error at path: " + proppath, e);
            }
        }
    }

    private Collection<KvProperty> getProps(T object) {
        Class<?> clazz = object.getClass();
        Map<String, KvProperty> p = this.props.get(clazz);
        if (p == null || CollectionUtils.isEmpty(this.props.get(clazz).values())) {
            Map<String, KvProperty> map = this.mapper.loadProps(clazz, t ->  t);
            this.props.put(clazz, map);
            return map.values();
        } else {
            return this.props.get(clazz).values();
        }
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
        return list.stream().map(k -> getName(k)).collect(Collectors.toList());
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
        Class<S> actualType = resolveType(path, type);
        S object = actualType.cast(factory.create(name, actualType));
        load(name, object);
        return actualType.cast(object);
    }

    @SuppressWarnings("unchecked")
    private <S extends T> Class<S> resolveType(String path, Class<S> subType) {
        Class<S> actualType = (Class<S>) this.type;
        if(subType != null) {
            Assert.isTrue(this.type.isAssignableFrom(subType), "Specified type " + subType + " must be an subtype of " + this.type);
            actualType = subType;
        }
        Class<S> savedType = loadType(path);
        if(savedType != null) {
            actualType = savedType;
        }
        Class<S> jsonType = resolveJsonType(path, actualType);
        if(jsonType != null) {
            actualType = jsonType;
        }
        return actualType;
    }

    @SuppressWarnings("unchecked")
    private <S extends T> Class<S> loadType(String path) {
        KvNode node = this.storage.get(KvUtils.join(path, PROP_TYPE));
        if(node == null) {
            return null;
        }
        String className = node.getValue();
        if(className == null) {
            return null;
        }
        Class<S> savedType;
        try {
            savedType = (Class<S>) Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            // if we save some type, and can not restore it again, it mean that something wrong
            throw new RuntimeException(e);
        }
        return savedType;
    }

    @SuppressWarnings("unchecked")
    private <S> Class<S> resolveJsonType(String path, Class<S> type) {
        JsonTypeInfo typeInfo = AnnotationUtils.findAnnotation(type, JsonTypeInfo.class);
        if (typeInfo == null) {
            return null;
        }
        String property = typeInfo.property();
        String proppath = KvUtils.join(path, property);
        try {
            KvNode node = this.storage.get(proppath);
            if(node == null) {
                return null;
            }
            String str = fromJsonString(node.getValue());
            JsonSubTypes subTypes = AnnotationUtils.findAnnotation(type, JsonSubTypes.class);
            for (JsonSubTypes.Type t : subTypes.value()) {
                if (t.name().equals(str)) {
                    return (Class<S>) t.value();
                }
            }
        } catch (Exception e) {
            log.error("can't instantiate class", e);
        }
        return null;
    }

    private String fromJsonString(String value) {
        if(value == null) {
            return null;
        }
        int end = value.length() - 1;
        // note that it method used only for type names, it does not support escape sequences and etc.
        if(value.charAt(0) != '"' || value.charAt(end) != '"') {
            throw new IllegalArgumentException("Invalid json string: " + value);
        }
        return value.substring(1, end);
    }

    /**
     * Load into existed object
     * @param name
     * @param object
     */
    public void load(String name, T object) {
        String path = path(name);
        for(KvProperty property: getProps(object)) {
            String proppath = KvUtils.join(path, property.getKey());
            String str = null;
            try {
                KvNode node = this.storage.get(proppath);
                if(node != null) {
                    str = node.getValue();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error at path: " + proppath, e);
            }
            property.set(object, str);
        }
        Validity validity = mapper.validate(path, object);
        if(!validity.isValid()) {
            throw new ValidityException("Invalid : ", validity);
        }
    }
}
