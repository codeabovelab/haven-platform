/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.common.kv.mapping;

import com.codeabovelab.dm.common.kv.KvStorageEvent;
import com.codeabovelab.dm.common.kv.KvUtils;
import com.google.common.collect.ImmutableSet;
import lombok.Data;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * KeyValue storage map of directory. <p/>
 * It has internal cache on plain map, without timeouts, and also update it on KV events. We do not using
 * guava cache because want to add keys into map without loading values.
 */
public class KvMap<T> {
    @Data
    public static class Builder {
        private KvMapperFactory factory;
        private String path;

        public Builder factory(KvMapperFactory factory) {
            setFactory(factory);
            return this;
        }

        public Builder path(String path) {
            setPath(path);
            return this;
        }

        public <T> KvMap<T> build(Class<T> type) {
            return new KvMap<>(this, type);
        }
    }
    private final  class ValueHolder {
        private final String key;
        private volatile T value;
        private final Map<String, Long> index = new ConcurrentHashMap<>();
        private volatile boolean dirty = true;

        ValueHolder(String key) {
            this.key = key;
        }

        synchronized T save(T val) {
            this.dirty = false;
            if(val == value) {
                return value;
            }
            T old = this.value;
            this.value = val;
            mapper.save(key, val, (p, res) -> {
                index.put(p.getKey(), res.getIndex());
            });
            return old;
        }

        synchronized void dirty(String prop, long newIndex) {
            Long old = this.index.get(prop);
            if(old != null && old != newIndex) {
                dirty();
            }
        }

        synchronized void dirty() {
            this.dirty = true;
        }

        synchronized T get() {
            if(dirty) {
                load();
            }
            return value;
        }

        synchronized void set(T value) {
            this.dirty = false;
            this.value = value;
        }

        synchronized void load() {
            set(mapper.load(key));
        }
    }

    private final KvClassMapper<T> mapper;
    private final ConcurrentMap<String, ValueHolder> map = new ConcurrentHashMap<>();

    private KvMap(Builder builder, Class<T> type) {
        this.mapper = builder.factory.createClassMapper(builder.path, type);
        builder.factory.getStorage().subscriptions().subscribeOnKey(this::onKvEvent, builder.path);
    }

    public static Builder builder() {
        return new Builder();
    }

    private void onKvEvent(KvStorageEvent e) {
        String path = e.getKey();
        final long index = e.getIndex();
        KvStorageEvent.Crud action = e.getAction();
        String key = this.mapper.getName(path);
        String property = KvUtils.child(this.mapper.getPrefix(), path, 1);
        if(property != null) {
            getOrCreateHolder(key).dirty(property, index);
        } else {
            switch (action) {
                case CREATE:
                    // we use lazy loading
                    getOrCreateHolder(key);
                    break;
                case READ:
                    //ignore
                    break;
                case UPDATE:
                    getOrCreateHolder(key).dirty();
                    break;
                case DELETE:
                    map.remove(key);
            }
        }
    }

    /**
     * Get exists or load value from storage.
     * @param key key
     * @return value or null if not exists
     */
    public T get(String key) {
        // we can not use computeIfAbsent here because load code can modify map
        ValueHolder holder = getOrCreateHolder(key);
        T val = holder.get();
        if(val == null) {
            map.remove(key, holder);
            return null;
        }
        return holder.get();
    }

    /**
     * Put and save value into storage.
     * @param key key
     * @param val value
     * @return old value if exists
     */
    public T put(String key, T val) {
        ValueHolder holder = getOrCreateHolder(key);
        return holder.save(val);
    }

    /**
     * Remove value directly from storage, from map it will be removed at event.
     * @param key key for remove
     */
    public void remove(String key) {
        mapper.delete(key);
    }

    private ValueHolder getOrCreateHolder(String key) {
        ValueHolder holder = new ValueHolder(key);
        ValueHolder old = map.putIfAbsent(key, holder);
        if(old != null) {
            holder = old;
        }
        return holder;
    }

    /**
     * Immutable set of keys.
     * @return set of keys, never null.
     */
    public Set<String> list() {
        return ImmutableSet.copyOf(this.map.keySet());
    }

    private ValueHolder load(String key) {
        T val = mapper.load(key);
        if(val == null) {
            return null;
        }
        ValueHolder holder = new ValueHolder(key);
        holder.save(val);
        return holder;
    }
}
