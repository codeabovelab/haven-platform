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

import com.codeabovelab.dm.common.kv.KeyValueStorage;
import com.codeabovelab.dm.common.validate.JsrValidityImpl;
import com.codeabovelab.dm.common.validate.Validity;
import com.codeabovelab.dm.common.utils.FindHandlerUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 */
@Component
public class KvMapperFactory {
    final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper objectMapper;
    private final KeyValueStorage storage;
    private final Map<Class<?>, FieldSetter> setters;
    private final Map<Class<?>, PropertyInterceptor> interceptors;
    private final Validator validator;

    @Autowired
    @SuppressWarnings("unchecked")
    public KvMapperFactory(ObjectMapper objectMapper, KeyValueStorage storage, TextEncryptor encryptor, Validator validator) {
        this.objectMapper = objectMapper;
        this.storage = storage;
        this.validator = validator;

        ImmutableMap.Builder<Class<?>, FieldSetter> builder = ImmutableMap.<Class<?>, FieldSetter>builder();
        builder.put(Map.class, (field, value) -> {
            Map fieldMap = (Map) field;
            fieldMap.clear();
            if (value != null) {
                fieldMap.putAll((Map)value);
            }
        });
        builder.put(Collection.class, (field, value) -> {
            Collection fieldColl = (Collection) field;
            fieldColl.clear();
            fieldColl.addAll((Collection)value);
        });
        setters = builder.build();
        interceptors = ImmutableMap.<Class<?>, PropertyInterceptor>builder()
          .put(PropertyCipher.class, new PropertyCipher(encryptor))
          .build();
    }

    <T> Map<String, T> loadProps(Class<?> clazz, Function<KvProperty, T> func) {
        ImmutableMap.Builder<String, T> b = ImmutableMap.builder();
        TypeFactory tf = TypeFactory.defaultInstance();
        while(clazz != null && !Object.class.equals(clazz)) {
            for(Field field: clazz.getDeclaredFields()) {
                KvMapping mapping = field.getAnnotation(KvMapping.class);
                if(mapping == null) {
                    continue;
                }
                JavaType javaType;
                String typeStr = mapping.type();
                if(!typeStr.isEmpty()) {
                    javaType = tf.constructFromCanonical(typeStr);
                } else {
                    javaType = tf.constructType(field.getGenericType());
                }
                KvProperty property = new KvProperty(this, field.getName(), field, javaType);
                b.put(property.getKey(), func.apply(property));
            }
            clazz = clazz.getSuperclass();
        }
        return b.build();
    }

    @SuppressWarnings("unchecked")
    <T> FieldSetter<T> getSetter(Class<T> type) {
        return FindHandlerUtil.findByClass(type, setters);
    }

    public <T> KvMapper<T> createMapper(T object, String path) {
        return new KvMapper<>(this, object, path);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public KeyValueStorage getStorage() {
        return storage;
    }

    public <T>  KvClassMapper<T> createClassMapper(String prefix, Class<T> type) {
        return new KvClassMapper<>(this, prefix, type);
    }

    public PropertyInterceptor[] getInterceptors(Class<? extends PropertyInterceptor>[] classes) {
        PropertyInterceptor[] instances = new PropertyInterceptor[classes.length];
        for(int i = 0; i < classes.length; ++i) {
            Class<? extends PropertyInterceptor> type = classes[i];
            PropertyInterceptor instance = this.interceptors.get(type);
            Assert.notNull(instance, "can not find interceptor of type: " + type);
            instances[i] = instance;
        }
        return instances;
    }

    public <T> Validity validate(String path, T object) {
        Set<ConstraintViolation<T>> res = validator.validate(object);
        Validity validity = new JsrValidityImpl(path, res);
        if(!validity.isValid()) {
            log.warn("Invalid {}", validity.getMessage());
        }
        return validity;
    }
}
