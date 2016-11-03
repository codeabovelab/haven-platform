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

import com.fasterxml.jackson.databind.JavaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 */
final class KvProperty implements KvPropertyContext {
    private final Field field;
    private final KvMapperFactory factory;
    private final String key;
    private final JavaType type;
    private final FieldSetter<Object> setter;
    private final PropertyInterceptor interceptors[];

    public KvProperty(KvMapperFactory factory, String key, Field field, JavaType javaType) {
        this.factory = factory;
        this.key = key;
        this.field = field;
        if(!field.isAccessible()) {
            field.setAccessible(true);
        }
        boolean isFinal = Modifier.isFinal(this.field.getModifiers());
        if(isFinal) {
            @SuppressWarnings("unchecked")
            FieldSetter<Object> setter = factory.getSetter((Class<Object>)field.getType());
            Assert.notNull(setter, field + " is final and we does not have setters for its type.");
            this.setter = setter;
        } else {
            this.setter = null;
        }
        this.type = javaType;
        this.interceptors = this.factory.getInterceptors(field.getAnnotation(KvMapping.class).interceptors());
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public JavaType getType() {
        return type;
    }

    public String get(Object root) {
        String valstr;
        try {
            Object val = field.get(root);
            if(val == null) {
                valstr = null;
            } else {
                valstr = this.factory.getObjectMapper().writeValueAsString(val);
            }
            for(PropertyInterceptor interceptor: interceptors) {
                valstr = interceptor.save(this, valstr);
            }
        } catch (Exception e) {
            throw new RuntimeException("When get value of " + field, e);
        }
        return valstr;
    }

    public void set(Object root, String stringval) {
        try {
            for(PropertyInterceptor interceptor: interceptors) {
                stringval = interceptor.read(this, stringval);
            }
            Object val;
            if(StringUtils.isEmpty(stringval)) {
                val = null;
            } else {
                val = this.factory.getObjectMapper().readValue(stringval, this.getType());
            }
            if(this.setter != null) {
                this.setter.set(field.get(root), val);
            } else {
                field.set(root, val);
            }
        } catch (Exception e) {
            throw new RuntimeException("When deserialize value of " + field, e);
        }
    }
}
