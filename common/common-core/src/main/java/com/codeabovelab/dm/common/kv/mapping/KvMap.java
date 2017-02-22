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
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * KeyValue storage map of directory. <p/>
 * It has internal cache on plain map, without timeouts, and also update it on KV events. We do not using
 * guava cache because want to add keys into map without loading values.
 */
@Slf4j
public class KvMap<T> {

    @Data
    public static class Builder<T, V> {
        private KvMapperFactory mapper;
        private String path;
        private KvMapAdapter<T> adapter = KvMapAdapter.direct();
        private final Class<T> type;
        private final Class<V> valueType;
        /**
         * Note that it invoke at events caused by map user. For events from KV storage use {@link #setListener(Consumer)}.
         */
        private Consumer<KvMapLocalEvent<T>> localListener;
        /**
         * Note that it handle events from KV storage. For events caused by map user use {@link #setLocalListener(Consumer)} .
         */
        private Consumer<KvMapEvent<T>> listener;
        private KvObjectFactory<V> factory;
        /**
         * Pass dirty value into adapter. Otherwise adapter receive null value.
         * <p/>
         * Default - false;
         */
        private boolean passDirty;

        public Builder(Class<T> type, Class<V> valueType) {
            Assert.notNull(type, "type is null");
            this.type = type;
            this.valueType = valueType;
        }

        public Builder<T, V> mapper(KvMapperFactory factory) {
            setMapper(factory);
            return this;
        }

        public Builder<T, V> path(String path) {
            setPath(path);
            return this;
        }

        public Builder<T, V> adapter(KvMapAdapter<T> adapter) {
            setAdapter(adapter);
            return this;
        }

        /**
         * Note that it invoke at events caused by map user. For events from KV storage use {@link #setListener(Consumer)}.
         * @param consumer handler for local events causet by invoking of map methods
         * @return this
         */
        public Builder<T, V> localListener(Consumer<KvMapLocalEvent<T>> consumer) {
            setLocalListener(consumer);
            return this;
        }

        /**
         * Note that it handle events from KV storage. For events caused by map user use {@link #setLocalListener(Consumer)} .
         * @param listener handler for KV storage events.
         * @return this
         */
        public Builder<T, V> listener(Consumer<KvMapEvent<T>> listener) {
            setListener(listener);
            return this;
        }

        public Builder<T, V> factory(KvObjectFactory<V> factory) {
            setFactory(factory);
            return this;
        }

        /**
         * Pass dirty value into adapter. Otherwise adapter receive null value.
         * <p/>
         * Default - false;
         * @param passDirty flag
         * @return this
         */
        public Builder<T, V> passDirty(boolean passDirty) {
            setPassDirty(passDirty);
            return this;
        }

        public KvMap<T> build() {
            Assert.notNull(type);
            return new KvMap<>(this);
        }
    }
    private final class ValueHolder {
        private final String key;
        private volatile T value;
        private final Map<String, Long> index = new ConcurrentHashMap<>();
        private volatile boolean dirty = true;
        private volatile boolean barrier = false;

        ValueHolder(String key) {
            Assert.notNull(key, "key is null");
            this.key = key;
        }

        T save(T val) {
            T old;
            KvMapLocalEvent.Action action;
            synchronized (this) {
                checkValue(val);
                // we must not publish dirty value
                old = getIfPresent();
                this.dirty = false;
                if(val == value) {
                    return value;
                }
                action = this.value == null ? KvMapLocalEvent.Action.CREATE : KvMapLocalEvent.Action.UPDATE;
                this.value = val;
            }
            onLocal(action, this, old, val);
            flush();
            return old;
        }

        void flush() {
            Object obj;
            synchronized (this) {
                if(this.value == null) {
                    // no value set, nothing to flush
                    return;
                }
                this.dirty = false;
                obj = adapter.get(this.key, this.value);
            }
            // Note that message will be concatenated with type of object by `Assert.isInstanceOf`
            Assert.isInstanceOf(mapper.getType(), obj, "Adapter " + adapter + " return object of inappropriate");
            Assert.notNull(obj, "Adapter " + adapter + " return null from " + this.value + " that is not allowed");
            mapper.save(key, obj, (name, res) -> {
                synchronized (this) {
                    index.put(toIndexKey(name), res.getIndex());
                }
            });
        }

        private String toIndexKey(String name) {
            return name == null? THIS : name;
        }

        synchronized void dirty(String prop, long newIndex) {
            Long old = this.index.get(toIndexKey(prop));
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

        private void checkValue(T value) {
            Assert.notNull(value, "Null value is not allowed");
        }

        synchronized void load() {
            if(barrier) {
                throw new IllegalArgumentException("Recursion detected.");
            }
            barrier = true;
            try {
                T old = (dirty && !passDirty)? null : value;
                Object obj = mapper.load(key, adapter.getType(old));
                T newVal = null;
                if(obj != null || old != null) {
                    newVal = adapter.set(this.key, old, obj);
                    if(newVal == null) {
                        throw new IllegalStateException("Adapter " + adapter + " broke contract: it return null value for non null object.");
                    }
                }
                this.dirty = false;
                //here we must raise local event, but need to use another action like LOAD or SET,
                // UPDATE and CREATE - is not acceptable here
                this.value = newVal;
                onLocal(KvMapLocalEvent.Action.LOAD, this, old, newVal);
            } finally {
                barrier = false;
            }
        }

        synchronized T getIfPresent() {
            if(dirty) {
                // returning dirty value may cause unexpected effects
                return null;
            }
            return value;
        }

        synchronized T computeIfAbsent(Function<String, ? extends T> func) {
            get(); // we must try to load before compute
            if(value == null) {
                save(func.apply(key));
            }
            // get - is load value if its present, but dirty
            return get();
        }

        synchronized T compute(BiFunction<String, ? super T, ? extends T> func) {
            get(); // we must try to load before compute
            T newVal = func.apply(key, value);
            if(newVal != null) {
                save(newVal);
            } else {
                return null;
            }
            // get - is load value if its present, but dirty
            return get();
        }
    }

    /**
     * Used for replace this property in index map
     */
    static final String THIS = " this";
    private final KvClassMapper<Object> mapper;
    private final KvMapAdapter<T> adapter;
    private final Consumer<KvMapLocalEvent<T>> localListener;
    private final Consumer<KvMapEvent<T>> listener;
    private final Map<String, ValueHolder> map = new LinkedHashMap<>();
    private final boolean passDirty;

    @SuppressWarnings("unchecked")
    private KvMap(Builder builder) {
        Assert.notNull(builder.mapper, "mapper is null");
        Assert.notNull(builder.path, "path is null");
        this.adapter = builder.adapter;
        this.localListener = builder.localListener;
        this.listener = builder.listener;
        this.passDirty = builder.passDirty;
        Assert.isTrue(!this.passDirty || this.adapter != KvMapAdapter.DIRECT, "Direct adapter does not support passDirty flag.");
        Class<Object> mapperType = MoreObjects.firstNonNull(builder.valueType, (Class<Object>)builder.type);
        this.mapper = builder.mapper.buildClassMapper(mapperType)
          .prefix(builder.path)
          .factory(builder.factory)
          .build();
        builder.mapper.getStorage().subscriptions().subscribeOnKey(this::onKvEvent, builder.path);
    }

    /**
     * Load all data.
     */
    public void load() {
        this.mapper.list().forEach(this::getOrCreateHolder);
    }

    public static <T> Builder<T, T> builder(Class<T> type) {
        return new Builder<>(type, type);
    }

    public static <T, V> Builder<T, V> builder(Class<T> type, Class<V> value) {
        return new Builder<>(type, value);
    }

    private void onKvEvent(KvStorageEvent e) {
        String path = e.getKey();
        final long index = e.getIndex();
        KvStorageEvent.Crud action = e.getAction();
        String key = this.mapper.getName(path);
        if(key == null) {
            if(action == KvStorageEvent.Crud.DELETE) {
                // it meat that someone remove mapped node with all entries, we must clear map
                // note that current implementation does not support consistency
                List<ValueHolder> set;
                synchronized (map) {
                    set = new ArrayList<>(map.values());
                    map.clear();
                }
                set.forEach((holder) -> {
                    onLocal(KvMapLocalEvent.Action.DELETE, holder, holder.getIfPresent(), null);
                    invokeListener(KvStorageEvent.Crud.DELETE, holder.key, holder);
                });
            }
            return;
        }
        String property = KvUtils.child(this.mapper.getPrefix(), path, 1);
        ValueHolder holder = null;
        if(property != null) {
            holder = getOrCreateHolder(key);
            holder.dirty(property, index);
        } else {
            switch (action) {
                case CREATE:
                    // we use lazy loading
                    holder = getOrCreateHolder(key);
                    break;
                case READ:
                    //ignore
                    break;
                case UPDATE:
                    holder = getOrCreateHolder(key);
                    holder.dirty(null, index);
                    break;
                case DELETE:
                    synchronized (map) {
                        holder = map.remove(key);
                        if(holder != null) {
                            onLocal(KvMapLocalEvent.Action.DELETE, holder, holder.getIfPresent(), null);
                        }
                    }
            }
        }
        invokeListener(action, key, holder);
    }

    private void invokeListener(KvStorageEvent.Crud action, String key, ValueHolder holder) {
        if(listener != null) {
            T value = null;
            if(holder != null) {
                // we cah obtain value before it become dirty, but it will wrong behavior
                value = holder.getIfPresent();
            }
            listener.accept(new KvMapEvent<>(this, action, key, value));
        }
    }

    private void onLocal(KvMapLocalEvent.Action action, ValueHolder holder, T oldValue, T newValue) {
        if(localListener == null) {
            return;
        }
        KvMapLocalEvent<T> event = new KvMapLocalEvent<>(this, action, holder.key, oldValue, newValue);
        localListener.accept(event);
    }

    /**
     * Get exists or load value from storage.
     * @param key key
     * @return value or null if not exists
     */
    public T get(String key) {
        ValueHolder holder = getOrCreateHolder(key);
        T val = holder.get();
        if(val == null) {
            synchronized (map) {
                map.remove(key, holder);
            }
            return null;
        }
        return val;
    }

    /**
     * Get exists value from storage. Not load it, even if dirty.
     * @param key key
     * @return value or null if not exists or dirty.
     */
    public T getIfPresent(String key) {
        ValueHolder holder;
        synchronized (map) {
            holder = map.get(key);
        }
        if(holder == null) {
            return null;
        }
        return holder.getIfPresent();
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
     *
     * @param key key for remove
     * @return gives value only if present, not load it, this mean that you may obtain null, event storage has value
     */
    public T remove(String key) {
        ValueHolder valueHolder;
        synchronized (map) {
            valueHolder = map.get(key);
            // we not delete holder here, it mus tbe deleter from kv-event listener
        }
        mapper.delete(key);
        if (valueHolder != null) {
            // we must not load value
            return valueHolder.getIfPresent();
        }
        return null;
    }

    public T computeIfAbsent(String key, Function<String, ? extends T> func) {
        ValueHolder holder = getOrCreateHolder(key);
        return  holder.computeIfAbsent(func);
    }

    /**
     * Invoke on key. If value exystse then passed to fun, otherwise null. If funt return null value will be removed.
     * @param key key
     * @param func handler
     * @return new value
     */
    public T compute(String key, BiFunction<String, ? super T, ? extends T> func) {
        ValueHolder holder = getOrCreateHolder(key);
        T newVal = holder.compute(func);
        if(newVal == null) {
            synchronized (map) {
                map.remove(key, holder);
            }
        }
        return newVal;
    }

    /**
     * Save existed value of specified key. If is not present, do nothing.
     * @param key key of value.
     */
    public void flush(String key) {
        ValueHolder holder;
        synchronized (map) {
            holder = map.get(key);
        }
        if(holder != null) {
            holder.flush();
        }
    }

    private ValueHolder getOrCreateHolder(String key) {
        synchronized (map) {
            return map.computeIfAbsent(key, ValueHolder::new);
        }
    }

    /**
     * Gives Immutable set of keys. Note that it not load keys from storage. <p/>
     * For load keys you need to use {@link #load()}.
     * @return set of keys, never null.
     */
    public Set<String> list() {
        synchronized (map) {
            return ImmutableSet.copyOf(this.map.keySet());
        }
    }

    /**
     * Load all values of map. Note that it may cause time consumption.
     * @return immutable collection of values
     */
    public Collection<T> values() {
        ImmutableList.Builder<T> b = ImmutableList.builder();
        synchronized (map) {
            this.map.values().forEach(valueHolder -> {
                T element = safeGet(valueHolder);
                // map does not contain holders with null elements, but sometime it happen
                // due to multithread access , for example in `put()` method
                if(element != null) {
                    b.add(element);
                }
            });
        }
        return b.build();
    }

    public void forEach(BiConsumer<String, ? super T> action) {
        // we use copy for prevent call external code in lock
        Map<String, ValueHolder> copy;
        synchronized (map) {
            copy = new LinkedHashMap<>(this.map);
        }
        copy.forEach((key, holder) -> {
            T value = safeGet(holder);
            if(value != null) {
                action.accept(key, value);
            }
        });
    }

    private T safeGet(ValueHolder valueHolder) {
        T element = null;
        try {
            element = valueHolder.get();
        } catch (Exception e) {
            log.error("Can not load {}", valueHolder.key, e);
        }
        return element;
    }
}
