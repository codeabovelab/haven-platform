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
/*
import com.codeabovelab.dm.common.cache.CacheConfig;
import com.codeabovelab.dm.common.cache.ConfigurableCacheManager;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;

public class ConfigurableRedisCacheManager extends RedisCacheManager implements ConfigurableCacheManager {

    private CacheConfig defaultCacheConfig;

    public ConfigurableRedisCacheManager(CacheConfig defaultCacheConfig, RedisTemplate template) {
        super(template);
    }

    @Override
    public CacheConfig getDefaultConfiguration() {
        if(defaultCacheConfig != null) {
            return defaultCacheConfig;
        }
        // terrible design of RedisCacheManager does not allow us to get 'defaultExpiration' field
        return CacheConfig.builder().expireAfterWrite(computeExpiration("")).build();
    }

    @Override
    public Cache getCache(CacheConfig config) {
        String name = config.getName();
        Assert.hasText(name);
        addCache(createCache(config));
        return super.getCache(name);
    }

    @SuppressWarnings("unchecked")
    private Cache createCache(CacheConfig config) {
        String cacheName = config.getName();
        long expireAfterWrite = config.getExpireAfterWrite();
        if(expireAfterWrite < 0) {
            expireAfterWrite = computeExpiration(cacheName);
        }
        return new RedisCache(cacheName, (isUsePrefix() ? getCachePrefix().prefix(cacheName) : null), getRedisOperations(), expireAfterWrite);
    }
}
*/
