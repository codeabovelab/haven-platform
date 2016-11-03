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

package com.codeabovelab.dm.common.json;

import com.codeabovelab.dm.common.utils.pojo.PojoUtils;
import com.codeabovelab.dm.common.utils.pojo.Property;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Jackson transformation support
 */
public class JtModule extends Module {
    private static final Version VERSION = new Version(1, 0, 0, "", null, null);

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
        context.appendAnnotationIntrospector(new AnnotationIntrospectorImpl());
    }

    private class AnnotationIntrospectorImpl extends AnnotationIntrospector {
        final Version VERSION = new Version(1, 0, 0, "", null, null);

        @Override
        public Version version() {
            return VERSION;
        }

        @Override
        public Object findSerializationConverter(Annotated a) {
            JtToMap ann = a.getAnnotation(JtToMap.class);
            if (ann == null) {
                return null;
            }
            return new SerializationConverterImpl(ann, new Ctx(a, a.getType()));
        }

        @Override
        public Object findDeserializationConverter(Annotated a) {
            JtToMap ann = a.getAnnotation(JtToMap.class);
            if (ann == null) {
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
            return new DeserializationConverterImpl(ann, new Ctx(a, javaType));
        }
    }

    private static class Ctx {

        private final Annotated annotated;
        private final JavaType javaType;

        Ctx(Annotated annotated, JavaType javaType) {
            this.annotated = annotated;
            this.javaType = javaType;
        }

        public String getKey() {
            return annotated.getName();
        }

        public JavaType getType() {
            return javaType;
        }
    }

    private class SerializationConverterImpl implements Converter<Collection<Object>, Map<String, Object>> {

        private final Ctx pc;
        private final JtToMap ann;
        private final Property keyProp;

        SerializationConverterImpl(JtToMap ann, Ctx pc) {
            this.pc = pc;
            this.ann = ann;
            this.keyProp = getKeyProp(ann, pc);
        }

        @Override
        public Map<String, Object> convert(Collection<Object> value) {
            Map<String, Object> map = new LinkedHashMap<>();
            value.forEach(o -> {
                String key = (String)keyProp.get(o);
                Object old = map.put(key, o);
                Assert.isNull(old, "Multiple objects with same key: " + key + " one: " + old + " two: " + o);
            });
            return map;
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return pc.getType();
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return constructMapType(this.pc, typeFactory);
        }

    }

    private MapType constructMapType(Ctx pc, TypeFactory typeFactory) {
        return typeFactory.constructMapType(LinkedHashMap.class, typeFactory.constructType(String.class), pc.getType().getContentType());
    }

    private Property getKeyProp(JtToMap ann, Ctx pc) {
        String key = ann.key();
        Assert.hasText(key, "key must has text");
        return PojoUtils.load(pc.getType().getContentType().getRawClass()).get(key);
    }

    private class DeserializationConverterImpl implements Converter<Map<String, Object>, List<Object>> {

        private final Ctx pc;
        private final JtToMap ann;
        private final Property keyProp;

        DeserializationConverterImpl(JtToMap ann, Ctx pc) {
            this.pc = pc;
            this.ann = ann;
            this.keyProp = getKeyProp(ann, pc);
        }

        @Override
        public List<Object> convert(Map<String, Object> value) {
            Set<Map.Entry<String, Object>> entries = value.entrySet();
            ArrayList<Object> res = new ArrayList<>(entries.size());
            entries.forEach(e -> {
                String key = e.getKey();
                Object val = e.getValue();
                keyProp.set(val, key);//we must update key prop, because it most significant than specified in object
                res.add(val);
            });
            return res;
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return constructMapType(this.pc, typeFactory);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return pc.getType();
        }
    }
}
