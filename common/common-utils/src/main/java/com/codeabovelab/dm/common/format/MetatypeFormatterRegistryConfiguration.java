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

package com.codeabovelab.dm.common.format;

import com.codeabovelab.dm.common.utils.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.*;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Platform supports two types of formatters framework:
 * 1. spring: org.springframework.format.FormatterRegistry (can be used only via annotaions)
 * 2. more generic: platform: com.codeabovelab.dm.common.format.MetatypeFormatterRegistry (can be used directly w/o annotations)
 * which is compatible with spring
 * this configurations adds formatters to both frameworks
 */
@Configuration
@ComponentScan
public class MetatypeFormatterRegistryConfiguration {

    @Autowired(required = false)
    private FormatterRegistry formatterRegistry;

    @Bean
    MetatypeFormatterRegistry metatypeFormatterRegistry() {
        return new DefaultMetatypeFormatterRegistry();
    }

    /**
     * The single point for register all common formatters to our MetatypeFormatterRegistry and spring FormatterRegistry
     * @param metatypeFormatterRegistry
     * @param formatters
     */
    @Autowired
    @SuppressWarnings("unchecked")
    void configureMetatypeFormatterRegistry(MetatypeFormatterRegistry metatypeFormatterRegistry,
                                            List<Formatter<?>> formatters) {
        for(final Formatter<?> formatter: formatters) {
            Set<Key<?>> set = FormatterUtils.getHandledMetatypes(formatter);
            if(set.isEmpty()) {
                continue;
            }
            final Set<Class<?>> types = new HashSet<>();
            for(Key<?> key: set) {
                types.add(key.getType());
                metatypeFormatterRegistry.addFormatter(key, (Formatter)formatter);
            }
            if(formatterRegistry != null) {
                formatterRegistry.addFormatterForFieldAnnotation(new AnnotationAnnotationFormatterFactory(types, formatter));
            }
        }
    }

    private static class AnnotationAnnotationFormatterFactory implements AnnotationFormatterFactory<Annotation> {
        private final Set<Class<?>> types;
        private final Formatter<?> formatter;

        public AnnotationAnnotationFormatterFactory(Set<Class<?>> types, Formatter<?> formatter) {
            this.types = Collections.unmodifiableSet(types);
            this.formatter = formatter;
        }

        @Override
        public Set<Class<?>> getFieldTypes() {
            return types;
        }

        @Override
        public Printer<?> getPrinter(Annotation annotation, Class<?> fieldType) {
            return formatter;
        }

        @Override
        public Parser<?> getParser(Annotation annotation, Class<?> fieldType) {
            return formatter;
        }
    }
}
