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

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;

/**
 * a property, getter-setter pair or public object field
 *
 */
public interface Property {
    /**
     * name of property
     * @return
     */
    String getName();

    /**
     * type of property
     * @return
     */
    Class<?> getType();

    /**
     * get property value from property owner object
     * @param owner
     * @return
     */
    Object get(Object owner);

    /**
     * is writable
     * @return exists
     */
    boolean isWritable();

    boolean isReadable();

    /**
     * set property value to property owner object
     * @param owner
     * @param value
     */
    void set(Object owner, Object value);

    /**
     * @see Member#getDeclaringClass()
     * @return
     */
    Class<?> getDeclaringClass();
    <T extends Annotation> T getAnnotation(Class<T> annotation);
    boolean isAnnotationPresent(Class<? extends Annotation> annotation);
}