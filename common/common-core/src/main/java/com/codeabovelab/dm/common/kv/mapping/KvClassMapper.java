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

import com.codeabovelab.dm.common.validate.Validity;
import com.codeabovelab.dm.common.validate.ValidityException;
import com.codeabovelab.dm.common.kv.DeleteDirOptions;
import com.codeabovelab.dm.common.kv.KeyValueStorage;
import com.codeabovelab.dm.common.kv.KvUtils;
import com.codeabovelab.dm.common.kv.WriteOptions;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 */
@Slf4j
public class KvClassMapper<T> {

    private final Class<T> type;
    private final String prefix;
    private final KvMapperFactory factory;
    private final Map<Class, Map<String, KvProperty>> props = new HashMap<>();
    private final KeyValueStorage storage;

    KvClassMapper(KvMapperFactory factory, String prefix, Class<T> type) {
        this.factory = factory;
        this.prefix = prefix;
        this.type = type;
        Map<String, KvProperty> map = this.factory.loadProps(type, p -> p);
        this.props.put(type, map);
        this.storage = factory.getStorage();
    }

    public void delete(String name) {
        this.storage.deletedir(path(name), DeleteDirOptions.builder().recursive(true).build());
    }

    private String path(String name) {
        return KvUtils.join(this.prefix, name);
    }

    public void save(String name, T object) {
        String path = path(name);

        for(KvProperty property: getProps(object)) {
            String strval = property.get(object);
            String proppath = KvUtils.join(path, property.getKey());
            try {
                this.storage.set(proppath, strval);
            } catch (Exception e) {
                throw new RuntimeException("Error at path: " + proppath, e);
            }
        }
    }

    private Collection<KvProperty> getProps(T object) {
        Class<?> clazz = object.getClass();
        Map<String, KvProperty> p = this.props.get(clazz);
        if (p == null || CollectionUtils.isEmpty(this.props.get(clazz).values())) {
            Map<String, KvProperty> map = this.factory.loadProps(clazz, t ->  t);
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
     *
     * @param name can be obtainer from {@link #list()}
     * @return
     */
    public T load(String name) {
        JsonTypeInfo typeInfo = AnnotationUtils.findAnnotation(this.type, JsonTypeInfo.class);
        if (typeInfo != null) {
            String property = typeInfo.property();
            String path = path(name);
            String proppath = KvUtils.join(path, property);
            try {
                String str = this.storage.get(proppath);
                JsonSubTypes subTypes = AnnotationUtils.findAnnotation(this.type, JsonSubTypes.class);
                for (JsonSubTypes.Type t : subTypes.value()) {
                    if (t.name().equals(str.replace("\"", ""))) {
                        @SuppressWarnings("unchecked")
                        T object = (T) BeanUtils.instantiate(t.value());
                        load(name, object);
                        return object;
                    }
                }
            } catch (Exception e) {
                log.error("can't instantiate class", e);
            }
        }
        T object = BeanUtils.instantiate(this.type);
        load(name, object);
        return object;
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
            String str;
            try {
                str = this.storage.get(proppath);
            } catch (Exception e) {
                throw new RuntimeException("Error at path: " + proppath, e);
            }
            property.set(object, str);
        }
        Validity validity = factory.validate(path, object);
        if(!validity.isValid()) {
            throw new ValidityException("Invalid : ", validity);
        }
    }
}
