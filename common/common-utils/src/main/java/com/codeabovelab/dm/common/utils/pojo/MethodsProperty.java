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

import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * property represented by getter and setter methods
 *
 */
public final class MethodsProperty implements Property {

    public static final class Builder {
        private final String name;
        private Method getter;
        private Method setter;

        Builder(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Method getGetter() {
            return getter;
        }

        public void setGetter(Method getter) {
            this.getter = getter;
        }

        public Method getSetter() {
            return setter;
        }

        public void setSetter(Method setter) {
            this.setter = setter;
        }

        public MethodsProperty build() {
            return new MethodsProperty(this);
        }
    }

    private final String name;
    private final Method getter;
    private final Method setter;

    MethodsProperty(Builder b) {
        this.name = b.name;
        this.getter = b.getter;
        this.setter = b.setter;
        if(this.getter == null && this.setter == null) {
            throw new NullPointerException("getter and setter is null");
        }
    }

    public static Builder build(String name) {
        return new Builder(name);
    }

    @Override
    public Class<?> getType() {
        Method m = this.getter;
        if(m == null) {
            m = this.setter;
        }
        return m.getReturnType();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isReadable() {
        return this.getter != null;
    }

    @Override
    public Object get(Object owner) {
        try {
            return this.getter.invoke(owner);
        } catch(ReflectiveOperationException e) {
            throw new RuntimeException("call " + this.getter + " on " + owner);
        }
    }

    @Override
    public boolean isWritable() {
        return setter != null;
    }

    @Override
    public void set(Object owner, Object value) {
        try {
            this.setter.invoke(owner, value);
        } catch(ReflectiveOperationException | IllegalArgumentException e) {
            throw new RuntimeException("Call " + this.setter + " on " + owner + " with arg " + value);
        }
    }

    @Override
    public Class<?> getDeclaringClass() {
        //note that method can be declared in different classes, and we use more specific class
        Class<?> gc = getter == null? null : getter.getDeclaringClass();
        Class<?> sc = setter == null? null : setter.getDeclaringClass();
        Class<?> dc = gc;
        if(sc != null && dc != sc && (dc == null || dc.isAssignableFrom(sc))) {
            dc = sc;
        }
        return dc;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotation) {
        T ann = getter == null? null : AnnotationUtils.findAnnotation(getter, annotation);
        if(ann == null && setter != null) {
            ann = AnnotationUtils.findAnnotation(setter, annotation);
        }
        return ann;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        return (getter != null && AnnotationUtils.findAnnotation(getter, annotation) != null) ||
          (setter != null && AnnotationUtils.findAnnotation(setter, annotation) != null);
    }
}