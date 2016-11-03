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

package com.codeabovelab.dm.platform.configuration;

import com.codeabovelab.dm.common.cache.CacheConfig;
import com.codeabovelab.dm.common.cache.CacheManagerProperties;
import com.codeabovelab.dm.common.cache.DmCachingConfigurer;
import com.codeabovelab.dm.platform.cache.ConfigurableGuavaCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@EnableCaching
@Configuration
@ComponentScan(basePackageClasses = {DmCachingConfigurer.class})
@EnableConfigurationProperties({CacheConfiguration.LocalCacheManagerProperties.class, CacheConfiguration.DistributedCacheManagerProperties.class})
public class CacheConfiguration {

    public static final String DISTRIBUTED_CACHE_MANAGER = "distributedCacheManager";

    @ConfigurationProperties("cache.localCacheManager")
    static class LocalCacheManagerProperties extends CacheManagerProperties {  }

    @ConfigurationProperties("cache.distributedCacheManager")
    static class DistributedCacheManagerProperties extends CacheManagerProperties { }

    /**
     * default expiration time (by default cache is disabled)
     */
    @Value("${default.cache.expiration:0}") //milliseconds
    private Long defaultExpiration;

    /**
     * Redis cache
     * @param properties
     * @param redisTemplate
     * @return
     */
    /*
    @Bean(name = DISTRIBUTED_CACHE_MANAGER)
    @Profile("redis")
    public CacheManager distributedCacheManager(DistributedCacheManagerProperties properties, @Qualifier("cacheTemplate") RedisTemplate redisTemplate) {
        ConfigurableRedisCacheManager cacheManager = new ConfigurableRedisCacheManager(configureDefault(properties).build(), redisTemplate);
        properties.configureCaches(cacheManager);
        return cacheManager;
    }
    */


    /**
     * Default cache: local (guava)
     * @param properties
     * @return
     */
    @Bean
    @Primary
    public CacheManager localCacheManager(LocalCacheManagerProperties properties) {
        ConfigurableGuavaCacheManager cacheManager = new ConfigurableGuavaCacheManager(configureDefault(properties).build());
        properties.configureCaches(cacheManager);
        return cacheManager;
    }

    private CacheConfig.Builder configureDefault(CacheManagerProperties properties) {
        CacheConfig.Builder builder = CacheConfig.builder();
        builder.from(properties);
        // use defaultExpiration if configured is negative
        if(defaultExpiration >= 0 && builder.getExpireAfterWrite() < 0) {
            builder.setExpireAfterWrite(defaultExpiration);
        }
        return builder;
    }

    public void setDefaultExpiration(Long defaultExpiration) {
        this.defaultExpiration = defaultExpiration;
    }
}
