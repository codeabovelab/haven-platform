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

package com.codeabovelab.dm.cluman.job;

import com.codeabovelab.dm.common.utils.pojo.FieldProperty;
import com.codeabovelab.dm.common.utils.pojo.MethodsProperty;
import com.codeabovelab.dm.common.utils.pojo.PojoUtils;
import com.codeabovelab.dm.common.utils.pojo.Property;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
final class JobBeanIntrospector {

    static class Metadata {
        private final Class<?> clazz;
        private final Map<String, PropertyMetadata> props;
        private final List<Dependency> deps;

        Metadata(Class<?> clazz, Map<String, PropertyMetadata> props, List<Dependency> deps) {
            this.clazz = clazz;
            this.props = ImmutableMap.copyOf(props);
            this.deps = ImmutableList.copyOf(deps);
        }

        Class<?> getClazz() {
            return clazz;
        }

        Map<String, PropertyMetadata> getProps() {
            return props;
        }

        List<Dependency> getDeps() {
            return deps;
        }
    }

    static class Dependency {
        private final String name;
        private final Class<?> type;

        public Dependency(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Class<?> getType() {
            return type;
        }
    }

    static final class PropertyMetadata {
        private final String name;
        private final Property property;
        private final boolean required;
        private final boolean in;
        private final boolean out;

        PropertyMetadata(String name, Field field, boolean required, boolean in, boolean out) {
            this.name = name;
            this.property = new FieldProperty(field);
            this.required = required;
            this.in = in;
            this.out = out;
        }

        PropertyMetadata(String name, Property property, boolean required) {
            this.name = name;
            this.property = property;
            this.out = property.isReadable();
            this.in = property.isWritable();
            this.required = required;
        }

        String getName() {
            return name;
        }

        Property getProperty() {
            return property;
        }

        boolean isRequired() {
            return required;
        }

        boolean isIn() {
            return in;
        }

        boolean isOut() {
            return out;
        }

        public Type getType() {
            return property.getGenericType();
        }
    }

    private static final ConcurrentHashMap<Class<?>, Metadata> map = new ConcurrentHashMap<>();

    private JobBeanIntrospector() {
    }

    static Metadata getMetadata(Class<?> clazz) {
        boolean hasJobMetadata = clazz.isAnnotationPresent(JobBean.class) ||
          clazz.isAnnotationPresent(JobComponent.class) ||
          clazz.isAnnotationPresent(JobIterationComponent.class);
        if(!hasJobMetadata) {
            return null;
        }
        return map.computeIfAbsent(clazz, JobBeanIntrospector::createMetadata);
    }

    private static Metadata createMetadata(Class<?> clazz) {
        List<Dependency> deps = new ArrayList<>();
        Map<String, PropertyMetadata> metadataMap = new HashMap<>();
        ReflectionUtils.doWithFields(clazz, field -> {
            if((field.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) != 0) {
                return;
            }
            if (processDeps(deps, field)) {
                return;
            }
            JobParam ann = field.getAnnotation(JobParam.class);
            if(ann == null) {
                return;
            }
            String name = getName(field.getName(), ann);
            metadataMap.put(name, new PropertyMetadata(name, field, ann.required(), ann.in(), ann.out()));
        });

        Map<String, MethodsProperty.Builder> methodProps = PojoUtils.loadMethodPropertyBuilders(clazz);
        for(MethodsProperty.Builder builder: methodProps.values()) {
            Method setter = builder.getSetter();
            if(setter != null && processDeps(deps, setter)) {
                continue;
            }

            JobParam ann = null;
            Method getter = builder.getGetter();
            if(getter != null) {
                ann = getter.getAnnotation(JobParam.class);
            }
            if(setter != null) {
                ann = setter.getAnnotation(JobParam.class);
            }

            if(ann == null) {
                continue;
            }
            String name = getName(builder.getName(), ann);
            PropertyMetadata m = new PropertyMetadata(name, builder.build(), ann.required());
            metadataMap.put(m.getName(), m);
        }
        return new Metadata(clazz, metadataMap, deps);
    }

    private static boolean processDeps(List<Dependency> deps, AnnotatedElement ao) {
        if(!(AnnotatedElementUtils.isAnnotated(ao, Autowired.class))) {
            return false;
        }
        Qualifier qualifier = AnnotatedElementUtils.getMergedAnnotation(ao, Qualifier.class);
        String name = qualifier == null ? null : qualifier.value();
        Class<?> type = (ao instanceof Field)? ((Field)ao).getType() : ((Method)ao).getReturnType();
        deps.add(new Dependency(name, type));
        return true;
    }


    private static String getName(String defName, JobParam ann) {
        String name = ann.value();
        if(StringUtils.hasText(name)) {
            return name;
        }
        return defName;
    }
}
