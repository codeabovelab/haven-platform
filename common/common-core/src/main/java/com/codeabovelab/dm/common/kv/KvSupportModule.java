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

package com.codeabovelab.dm.common.kv;

import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.codeabovelab.dm.common.kv.mapping.KvPropertyContext;
import com.codeabovelab.dm.common.kv.mapping.PropertyInterceptor;
import com.codeabovelab.dm.common.utils.ArrayUtils;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;

import java.io.IOException;

/**
 */
public class KvSupportModule extends Module {

    private static final Version VERSION = new Version(1, 0, 0, "", null, null);
    private final KvMapperFactory factory;

    public KvSupportModule(KvMapperFactory factory) {
        this.factory = factory;
    }

    @Override
    public String getModuleName() {
        return getClass().getName();
    }

    @Override
    public Version version() {
        return VERSION;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.appendAnnotationIntrospector(new KvAnnotationIntrospector());
    }

    private class KvAnnotationIntrospector extends AnnotationIntrospector {
        final Version VERSION = new Version(1, 0, 0, "", null, null);

        @Override
        public Version version() {
            return null;
        }

        @Override
        public Object findSerializationConverter(Annotated a) {
            Class<? extends PropertyInterceptor>[] interceptors = getInterceptors(a);
            if (interceptors == null) {
                return null;
            }
            return new KvInterceptorsSerializationConverter(interceptors, new KvPropertyContextImpl(a, a.getType()));
        }

        @Override
        public Object findDeserializationConverter(Annotated a) {
            Class<? extends PropertyInterceptor>[] interceptors = getInterceptors(a);
            if (interceptors == null) {
                return null;
            }
            JavaType javaType = a.getType();
            if(a instanceof AnnotatedMethod) {
                AnnotatedMethod am = (AnnotatedMethod) a;
                if(am.getParameterCount() == 1) {
                    javaType = am.getParameterType(0);
                } else {
                    throw new RuntimeException("Invalid property setter: " + am.getAnnotated());
                }
            }
            return new KvInterceptorsDeserializationConverter(interceptors, new KvPropertyContextImpl(a, javaType));
        }

        private Class<? extends PropertyInterceptor>[] getInterceptors(Annotated a) {
            KvMapping ann = a.getAnnotation(KvMapping.class);
            if(ann == null) {
                return null;
            }
            Class<? extends PropertyInterceptor>[] interceptors = ann.interceptors();
            if(ArrayUtils.isEmpty(interceptors)) {
                return null;
            }
            return interceptors;
        }
    }

    private static class KvPropertyContextImpl implements KvPropertyContext {

        private final Annotated annotated;
        private final JavaType javaType;

        KvPropertyContextImpl(Annotated annotated, JavaType javaType) {
            this.annotated = annotated;
            this.javaType = javaType;
        }

        @Override
        public String getKey() {
            return annotated.getName();
        }

        @Override
        public JavaType getType() {
            return javaType;
        }
    }

    private class KvInterceptorsSerializationConverter implements Converter<Object, String> {

        private final KvPropertyContextImpl pc;
        private final PropertyInterceptor[] interceptors;

        KvInterceptorsSerializationConverter(Class<? extends PropertyInterceptor>[] interceptors, KvPropertyContextImpl pc) {
            this.pc = pc;
            this.interceptors = factory.getInterceptors(interceptors);
        }

        @Override
        public String convert(Object value) {
            ObjectMapper om = factory.getObjectMapper();
            String valstr;
            try {
                valstr = om.writeValueAsString(value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            for(PropertyInterceptor interceptor: interceptors) {
                valstr = interceptor.save(pc, valstr);
            }
            return valstr;
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return pc.getType();
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructType(String.class);
        }
    }

    private class KvInterceptorsDeserializationConverter implements Converter<String, Object> {

        private final KvPropertyContextImpl pc;
        private final PropertyInterceptor[] interceptors;

        KvInterceptorsDeserializationConverter(Class<? extends PropertyInterceptor>[] interceptors, KvPropertyContextImpl pc) {
            this.pc = pc;
            this.interceptors = factory.getInterceptors(interceptors);
        }

        @Override
        public Object convert(String value) {
            String valstr = value;
            for(PropertyInterceptor interceptor: interceptors) {
                valstr = interceptor.read(pc, valstr);
            }
            ObjectMapper om = factory.getObjectMapper();
            try {
                return om.readValue(valstr, pc.getType());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructType(String.class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return pc.getType();
        }
    }
}
