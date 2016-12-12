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

package com.codeabovelab.dm.common.utils.pojo;

import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 */
public final class FieldProperty implements Property {

    private final Field field;

    public FieldProperty(Field field) {
        this.field = field;
        ReflectionUtils.makeAccessible(this.field);
    }

    @Override
    public void set(Object obj, Object value) {
        try {
            field.set(obj, value);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new RuntimeException("On set field " + field + " on " + obj + " to value " + value, e);
        }
    }

    @Override
    public Object get(Object obj) {
        try {
            return field.get(obj);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("On get field " + field + " on " + obj, e);
        }
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public Class<?> getType() {
        return field.getType();
    }

    @Override
    public Type getGenericType() {
        return field.getGenericType();
    }

    @Override
    public boolean isWritable() {
        return !Modifier.isFinal(field.getModifiers());
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return field.getDeclaringClass();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotation) {
        return field.getAnnotation(annotation);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        return field.isAnnotationPresent(annotation);
    }
}
