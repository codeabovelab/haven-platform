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

package com.codeabovelab.dm.platform.cache;

import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.codeabovelab.dm.common.cache.CacheConfig;
import com.codeabovelab.dm.common.cache.ConfigurableCacheManager;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 */
public class ConfigurableGuavaCacheManager implements ConfigurableCacheManager {
    private final CacheConfig defaultConfiguration;
    private final ConcurrentMap<String, Supplier<GuavaCache>> caches = new ConcurrentHashMap<>();

    public ConfigurableGuavaCacheManager(CacheConfig defaultConfiguration) {
        this.defaultConfiguration = defaultConfiguration;
    }

    @Override
    public CacheConfig getDefaultConfiguration() {
        return defaultConfiguration;
    }

    @Override
    public Cache getCache(CacheConfig config) {
        return getCache(config.getName(), new GuavaCacheSupplier(config));
    }

    @Override
    public Cache getCache(String name) {
        return getCache(name, new GuavaCacheSupplier(name));
    }

    private Cache getCache(String name, GuavaCacheSupplier newSupplier) {
        Supplier<GuavaCache> supplier = caches.putIfAbsent(name, newSupplier);
        if(supplier == null) {
            supplier = newSupplier;
        }
        return supplier.get();
    }


    @Override
    public Collection<String> getCacheNames() {
        return new HashSet<>(caches.keySet());
    }

    final class GuavaCache implements Cache {
        private final CacheConfig cacheConfig;
        private final com.google.common.cache.Cache<Object, SimpleValueWrapper> cache;

        public GuavaCache(CacheConfig cacheConfig) {
            this.cacheConfig = cacheConfig;
            this.cache = buildCache(cacheConfig);
        }

        @Override
        public String getName() {
            return cacheConfig.getName();
        }

        @Override
        public Object getNativeCache() {
            return this.cache;
        }

        @Override
        public ValueWrapper get(Object key) {
            return cache.getIfPresent(key);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(Object key, Class<T> type) {
            ValueWrapper wrapper = cache.getIfPresent(key);
            if(wrapper == null) {
                return null;
            }
            Object value = wrapper.get();
            if(type == null) {
                return (T) value;
            }
            return type.cast(value);
        }

        @Override
        public <T> T get(Object key, Callable<T> callable) {
            throw new IllegalStateException("not implemented");
        }

        @Override
        public void put(Object key, Object value) {
            cache.put(key, new SimpleValueWrapper(value));
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            return cache.asMap().putIfAbsent(key, new SimpleValueWrapper(value));
        }

        @Override
        public void evict(Object key) {
            cache.invalidate(key);
        }

        @Override
        public void clear() {
            cache.invalidateAll();
        }

        @Override
        public String toString() {
            return "GuavaCache{" +
              "name=" + getName() +
              '}';
        }
    }

    private com.google.common.cache.Cache<Object, SimpleValueWrapper> buildCache(CacheConfig cacheConfig) {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        if(cacheConfig.getExpireAfterWrite() >= 0) {
            builder.expireAfterWrite(cacheConfig.getExpireAfterWrite(), TimeUnit.MILLISECONDS);
        }
        return builder.build();
    }

    private class GuavaCacheSupplier implements Supplier<GuavaCache> {
        private final CacheConfig config;
        private final String name;
        private volatile GuavaCache cache;

        public GuavaCacheSupplier(CacheConfig config) {
            this.config = config;
            this.name = this.config.getName();
        }

        public GuavaCacheSupplier(String name) {
            this.name = name;
            this.config = null;
        }

        @Override
        public GuavaCache get() {
            if(this.cache == null) {
                synchronized (this) {
                    if(this.cache == null) {
                        CacheConfig cfg = this.config;
                        if(cfg == null) {
                            cfg = CacheConfig.builder().from(getDefaultConfiguration()).name(this.name).build();
                        }
                        this.cache = new GuavaCache(cfg);
                    }
                }
            }
            return this.cache;
        }
    }
}
