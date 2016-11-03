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

import com.google.common.collect.ImmutableMap;
import com.codeabovelab.dm.common.utils.Key;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Default {@link com.codeabovelab.dm.common.gateway.ConnectionAttributes } implementation.
 */
public final class ConnectionAttributesImpl implements ConnectionAttributes {
    private final ConcurrentMap<Key<?>, Object> map = new ConcurrentHashMap<>();

    @Override
    public <T> T get(Key<T> key) {
        Object o = map.get(key);
        return cast(key, o);
    }

    private static <T> T cast(Key<T> key, Object o) {
        return key.getType().cast(o);
    }

    @Override
    public <T> T put(Key<T> key, T value) {
        Object old = map.put(key, cast(key, value));
        return cast(key, old);
    }

    @Override
    public <T> T computeIfAbsent(Key<T> key, Function<Key<?>, ?> function) {
        Object old = map.computeIfAbsent(key, function);
        return cast(key, old);
    }

    @Override
    public Map<Key<?>, Object> getMap() {
        return ImmutableMap.copyOf(map);
    }
}
