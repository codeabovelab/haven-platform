/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.common.kv.mapping;

import com.codeabovelab.dm.common.kv.KeyValueStorage;
import com.codeabovelab.dm.common.kv.KvNode;
import com.codeabovelab.dm.common.kv.KvUtils;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 */
@Slf4j
class NodeMapping<T> extends AbstractMapping<T> {
    private static final String PROP_TYPE = "@class";
    private final Map<Class, Map<String, KvProperty>> props = new HashMap<>();
    private final KvObjectFactory<T> factory;

    private NodeMapping(KvMapperFactory mapper, Class<T> type, Map<String, KvProperty> map, KvObjectFactory<T> factory) {
        super(mapper, type);
        this.props.put(type, map);
        this.factory = factory;
    }

    static <T> NodeMapping<T> makeIfHasProps(KvMapperFactory mapper, Class<T> type, KvObjectFactory<T> factory) {
        Map<String, KvProperty> map = mapper.loadProps(type, p -> p);
        if(map.isEmpty()) {
            return null;
        }
        return new NodeMapping<>(mapper, type, map, factory);
    }

    private Collection<KvProperty> getProps(T object) {
        Class<?> clazz = object.getClass();
        Map<String, KvProperty> p = this.props.get(clazz);
        if (p != null) {
            return p.values();
        }
        Map<String, KvProperty> map = this.mapper.loadProps(clazz, t ->  t);
        this.props.put(clazz, map);
        return map.values();
    }

    @Override
    void save(String path, T object, KvSaveCallback callback) {
        // we must read index of all node but, we can not create node in single command,
        // so we use index of last sub node
        Collection<KvProperty> props = getProps(object);
        if(props.isEmpty()) {
            throw new IllegalArgumentException("The path '" + path +
              "' is mapped to object of type " + object.getClass() + " which has no properties.");
        }
        //store type of object
        KeyValueStorage storage = getStorage();
        saveType(path, object, storage);
        //store properties
        for(KvProperty property: props) {
            String strval = property.get(object);
            String key = property.getKey();
            String proppath = KvUtils.join(path, key);
            try {
                KvNode res = storage.set(proppath, strval);
                if(callback != null) {
                    callback.call(key, res);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error at path: " + proppath, e);
            }
        }
    }

    @Override
    void load(String path, T object) {
        KeyValueStorage storage = getStorage();
        for(KvProperty property: getProps(object)) {
            String proppath = KvUtils.join(path, property.getKey());
            String str = null;
            try {
                KvNode node = storage.get(proppath);
                if(node != null) {
                    str = node.getValue();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error at path: " + proppath, e);
            }
            property.set(object, str);
        }
    }

    @Override
    <S extends T> S load(String path, String name, Class<S> type) {
        Class<S> actualType = resolveType(path, type);
        S object = actualType.cast(factory.create(name, actualType));
        load(path, object);
        return actualType.cast(object);
    }


    private <S extends T> Class<S> resolveType(String path, Class<S> actualType) {
        // we prefer json type mapping, and try load custom type only when no json mapping
        Class<S> jsonType = resolveJsonType(path, actualType);
        if(jsonType != null) {
            actualType = jsonType;
        } else {
            Class<S> savedType = loadType(path);
            if(savedType != null) {
                actualType = savedType;
            }
        }
        return actualType;
    }

    @SuppressWarnings("unchecked")
    private <S extends T> Class<S> loadType(String path) {
        KvNode node = getStorage().get(KvUtils.join(path, PROP_TYPE));
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
        String property = getPropertyName(typeInfo);
        String proppath = KvUtils.join(path, property);
        try {
            KvNode node = getStorage().get(proppath);
            if(node == null) {
                return null;
            }
            String str = node.getValue();
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

    private void saveType(String path, T object, KeyValueStorage storage) {
        Class<?> clazz = object.getClass();
        String name = PROP_TYPE;
        String value = clazz.getName();
        JsonTypeInfo typeInfo = AnnotationUtils.findAnnotation(clazz, JsonTypeInfo.class);
        if (typeInfo != null && !clazz.equals(typeInfo.defaultImpl())) {
            JsonTypeInfo.As include = typeInfo.include();
            if(include != JsonTypeInfo.As.PROPERTY &&
               include != JsonTypeInfo.As.EXTERNAL_PROPERTY /* it for capability with jackson oddities */) {
                throw new IllegalArgumentException("On " + clazz + " mapping support only " + JsonTypeInfo.As.PROPERTY + " but find: " + include);
            }
            name = getPropertyName(typeInfo);
            value = getJsonType(clazz, typeInfo);
        }
        storage.set(KvUtils.join(path, name), value);
    }

    private String getJsonType(Class<?> clazz, JsonTypeInfo typeInfo) {
        String value;
        JsonTypeInfo.Id use = typeInfo.use();
        switch (use) {
            case CLASS:
                value = clazz.getName();
                break;
            case NAME: {
                JsonSubTypes.Type needed = null;
                JsonSubTypes subTypes = AnnotationUtils.findAnnotation(clazz, JsonSubTypes.class);
                if(subTypes != null) {
                    for(JsonSubTypes.Type type: subTypes.value()) {
                        if(type.value().equals(clazz)) {
                            needed = type;
                            break;
                        }
                    }
                }
                if(needed == null) {
                    throw new IllegalArgumentException("On " + clazz + " can not find 'JsonSubTypes' record for current type.");
                }
                value = needed.name();
                break;
            }
            default:
                throw new IllegalArgumentException("On " + clazz + " find unexpected 'JsonTypeInfo.use' value: " + use);
        }
        return value;
    }

    private String getPropertyName(JsonTypeInfo typeInfo) {
        String property = typeInfo.property();
        if (property.isEmpty()) {
            JsonTypeInfo.Id use = typeInfo.use();
            property = use.getDefaultPropertyName();
        }
        return property;
    }

}
