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

package com.codeabovelab.dm.common.gateway;

import com.codeabovelab.dm.common.utils.Key;

import java.util.Map;
import java.util.function.Function;

/**
 * Map of connection attributes. <p/>
 * We use dedicated interface instead of map, because some implementations (like http session) does not provide to us map interface. <p/>
 * Also, note that attributes accessible in multithread environment.
 */
public interface ConnectionAttributes {

    /**
     * Return previously mapped value.
     * @param key
     * @param <T>
     * @return value or null
     */
    <T> T get(Key<T> key);

    /**
     * Store key-value pair into map
     * @param key
     * @param value
     * @param <T>
     * @return old value or null
     */
    <T> T put(Key<T> key, T value);

    /**
     * Return mapped value, ot if it is null or no mapped then calculate new value with 'function' and
     * map it with key.
     * @param key
     * @param function factory for new mapped value
     * @param <T>
     * @return mapped value, computed value or null if function return null
     */
    <T> T computeIfAbsent(Key<T> key, Function<Key<?>, ?> function);

    /**
     * Usually return immutable copy of all attributes.
     * @return
     */
    Map<Key<?>, Object> getMap();
}
