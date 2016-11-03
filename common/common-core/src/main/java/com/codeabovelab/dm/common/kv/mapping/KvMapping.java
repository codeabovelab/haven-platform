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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation of object fileds, which define mapping between fields and key value storage records. <p/>
 * Note that complex values (like list or map) must be immutable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface KvMapping {
    /**
     * Canonical type representation. It use fore json deserialization.
     * @see com.fasterxml.jackson.databind.type.TypeFactory#constructFromCanonical(String)
     * @see com.fasterxml.jackson.databind.JavaType#toCanonical()
     * @return
     */
    String type() default "";

    /**
     * Array of property interceptors which called in same order at save, and reverse order at read
     * @return
     */
    Class<? extends PropertyInterceptor>[] interceptors() default {};
}
