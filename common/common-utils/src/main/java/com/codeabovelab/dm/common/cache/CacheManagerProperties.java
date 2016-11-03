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

package com.codeabovelab.dm.common.cache;

import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * Common bean which can be used for configuration cache managers trough properties.
 * This class can not be final, because user can extent it.
 */
public class CacheManagerProperties extends CacheConfig.Builder {
    private Map<String, CacheConfig.Builder> caches;

    public void setCaches(Map<String, CacheConfig.Builder> caches) {
        this.caches = caches;
    }

    public Map<String, CacheConfig.Builder> getCaches() {
        return caches;
    }

    /**
     * It method configure caches defined in properties to cache manager.
     * @param cacheManager
     */
    public void configureCaches(ConfigurableCacheManager cacheManager) {
        if(CollectionUtils.isEmpty(this.caches)) {
            return;
        }
        for(Map.Entry<String, CacheConfig.Builder> entry: this.caches.entrySet()) {
            CacheConfig.Builder builder = entry.getValue();
            builder.setName(entry.getKey());
            cacheManager.getCache(builder.build());
        }
    }
}
