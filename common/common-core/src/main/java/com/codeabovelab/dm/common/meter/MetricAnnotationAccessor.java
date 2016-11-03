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

package com.codeabovelab.dm.common.meter;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.annotation.Counted;

import java.lang.annotation.Annotation;

/**
 * Interface and list of basic implementations of annotation accessors
 * it is used for not writing boilerplate code in com.codahale.metrics.annotation.Metered,
 * com.codahale.metrics.annotation.Timed,
 * com.ryantenney.metrics.annotation.Counted
 *
 * @param <T>
 */
interface MetricAnnotationAccessor<T extends Annotation> {

    MetricAnnotationAccessor<Metered> METERED = new MetricAnnotationAccessor<Metered>() {
        @Override
        public String getName(Metered annotation) {
            return annotation.name();
        }

        @Override
        public boolean isAbsoluteName(Metered annotation) {
            return annotation.absolute();
        }

        @Override
        public Class<Metered> getAnnotationType() {
            return Metered.class;
        }
    };

    MetricAnnotationAccessor<Timed> TIMED = new MetricAnnotationAccessor<Timed>() {
        @Override
        public String getName(Timed annotation) {
            return annotation.name();
        }

        @Override
        public boolean isAbsoluteName(Timed annotation) {
            return annotation.absolute();
        }

        @Override
        public Class<Timed> getAnnotationType() {
            return Timed.class;
        }
    };

    MetricAnnotationAccessor<Counted> COUNTED = new MetricAnnotationAccessor<Counted>() {
        @Override
        public String getName(Counted annotation) {
            return annotation.name();
        }

        @Override
        public boolean isAbsoluteName(Counted annotation) {
            return annotation.absolute();
        }

        @Override
        public Class<Counted> getAnnotationType() {
            return Counted.class;
        }
    };

    String getName(T annotation);
    boolean isAbsoluteName(T annotation);
    Class<T> getAnnotationType();
}
