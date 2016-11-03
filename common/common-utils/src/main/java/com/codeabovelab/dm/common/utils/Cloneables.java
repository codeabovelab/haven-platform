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

package com.codeabovelab.dm.common.utils;

import org.springframework.beans.BeanUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 */
public class Cloneables {

    /**
     * @param src
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(T src) {
        if(src == null) {
            return null;
        }
        Class<?> clazz = src.getClass();
        if(Map.class.isAssignableFrom(clazz)) {
            return (T) clone((Map<?, ?>) src);
        }
        if(Collection.class.isAssignableFrom(clazz)) {
            return (T) clone((Collection<?>) src);
        }
        if(Cloneable.class.isAssignableFrom(clazz)) {
            try {
                Method cloner = clazz.getDeclaredMethod("clone");
                return (T) cloner.invoke(src);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Can not clone instance of " + clazz, e);
            }
        }
        return src;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> clone(Map<K, V> src) {
        Map<K, V> map = BeanUtils.instantiate(src.getClass());
        src.forEach((k, v) -> map.put(clone(k), clone(v)));
        return map;
    }

    @SuppressWarnings("unchecked")
    private static <T> Collection<T> clone(Collection<T> src) {
        // also we may preallocate lists for src size
        Collection<T> collection = BeanUtils.instantiate(src.getClass());
        src.forEach(i -> collection.add(clone(i)));
        return src;
    }
}
