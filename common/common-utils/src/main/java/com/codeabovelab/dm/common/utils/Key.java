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

import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Map;

/**
 * The immutable key with type for any purposes.
 */
public final class Key<T> implements Serializable {
    private final Class<T> type;
    private final String name;

    public Key(Class<T> type) {
        this(type.getName(), type);
    }

    public Key(String name, Class<T> type) {
        Assert.notNull(name, "name is nul");
        this.name = name;
        Assert.notNull(type, "type is null");
        this.type = type;
    }

    public Class<T> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Key)) {
            return false;
        }

        Key key = (Key) o;

        if (name != null ? !name.equals(key.name) : key.name != null) {
            return false;
        }
        if (type != null ? !type.equals(key.type) : key.type != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Key{" +
          "type=" + type +
          ", name='" + name + '\'' +
          '}';
    }

    public static <T> T get(Map<Key<?>, ?> map, Key<T> key) {
        return key.cast(map.get(key));
    }

    public T cast(Object obj) {
        return type.cast(obj);
    }
}
