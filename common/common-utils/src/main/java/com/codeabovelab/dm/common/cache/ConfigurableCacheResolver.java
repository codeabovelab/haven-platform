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

import com.codeabovelab.dm.common.utils.ArrayUtils;
import com.codeabovelab.dm.common.utils.Throwables;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Cache resolver which allow to define cache with configuration via {@link com.codeabovelab.dm.common.cache.DefineCache }.
 */
public class ConfigurableCacheResolver implements CacheResolver {
    private final Map<String, CacheManager> cacheManagers;
    private final List<CacheInvalidator> cacheInvalidators;
    private final CacheManager defaultCacheManager;

    /**
     *
     * @param cacheManagers
     * @param defaultCacheManager optional default cache manager, usually it in memory local cache
     */
    public ConfigurableCacheResolver(Map<String, CacheManager> cacheManagers,
                                     CacheManager defaultCacheManager,
                                     List<CacheInvalidator> cacheInvalidators) {
        this.cacheManagers = cacheManagers;
        this.defaultCacheManager = defaultCacheManager;
        this.cacheInvalidators = cacheInvalidators;
    }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        BasicOperation operation = context.getOperation();
        Set<String> cacheNames = operation.getCacheNames();
        if (cacheNames.isEmpty()) {
            return Collections.emptyList();
        }
        if(this.cacheManagers.isEmpty() && defaultCacheManager == null) {
            throw new RuntimeException("No cache managers.");
        }
        final DefineCache defineCache = getAnnotation(context, DefineCache.class);
        String cacheManagerName = defineCache == null? null : defineCache.cacheManager();
        String definedName = getDefinedName(cacheNames, defineCache);
        CacheInvalidator invalidator = getInvalidator(defineCache);
        Map<String, String> invalidatorArgs = invalidator == null? null : getInvalidatorArgs(defineCache);
        Collection<Cache> result = new ArrayList<>();
        for (String cacheName : cacheNames) {
            Cache cache;
            final boolean define = Objects.equals(definedName, cacheName);
            CacheManager cacheManager = getCacheManager(cacheManagerName);
            if (define && cacheManager instanceof ConfigurableCacheManager) {
                cache = ((ConfigurableCacheManager)cacheManager).getCache(createConfig(defineCache, cacheName));
            } else {
                cache = cacheManager.getCache(cacheName);
            }
            if (cache == null) {
                throw new IllegalArgumentException("Cannot find cache named '" +
                  cacheName + "' for " + operation);
            }
            if(invalidator != null) {
                invalidator.init(cache, invalidatorArgs);
            }
            result.add(cache);
        }
        return result;
    }

    private Map<String, String> getInvalidatorArgs(DefineCache defineCache) {
        String[] argsarr = defineCache.invalidatorArgs();
        if(ArrayUtils.isEmpty(argsarr)) {
            return Collections.emptyMap();
        }
        if(argsarr.length % 2 != 0) {
            throw new IllegalArgumentException("Length of defineCache.invalidatorArgs must be an even.");
        }
        Map<String, String> map = new HashMap<>();
        for(int i = 0; i < argsarr.length;) {
            map.put(argsarr[i++], argsarr[i++]);
        }
        return Collections.unmodifiableMap(map);
    }

    private CacheInvalidator getInvalidator(DefineCache defineCache) {
        if(defineCache == null) {
            return null;
        }
        Class<? extends CacheInvalidator> clazz = defineCache.invalidator();
        if(CacheInvalidator.NullInvalidator.class.equals(clazz)) {
            return null;
        }
        for(CacheInvalidator invalidator: cacheInvalidators) {
            if(clazz.equals(invalidator.getClass())) {
                return invalidator;
            }
        }
        return null;
    }

    private <T extends Annotation> T getAnnotation(CacheOperationInvocationContext<?> context, Class<T> clazz) {
        try {
            // due to some cache proxy behaviour we can get method of superinterface instead of annotated method from target class
            // but sometime annotation has been appear on interface therefore we need check both cases
            Method proxiedMethod = context.getMethod();
            Class<?> targetClazz = context.getTarget().getClass();
            T annotation = null;
            if(!targetClazz.equals(proxiedMethod.getDeclaringClass())) {
                Method origMethod = targetClazz.getMethod(proxiedMethod.getName(), proxiedMethod.getParameterTypes());
                annotation = origMethod.getAnnotation(clazz);
            }
            if(annotation == null) {
                annotation = proxiedMethod.getAnnotation(clazz);
            }
            return annotation;
        } catch (NoSuchMethodException e) {
            throw Throwables.asRuntime(e);
        }
    }

    private CacheManager getCacheManager(String cacheManagerName) {
        CacheManager cacheManager = defaultCacheManager;
        if (!StringUtils.isEmpty(cacheManagerName)) {
            cacheManager = this.cacheManagers.get(cacheManagerName);
            if (cacheManager == null) {
                throw new RuntimeException("can not find cache manager with name: '" + cacheManagerName + "'");
            }
        }
        if(cacheManager == null) {
            throw new RuntimeException("Can not resolve cache manager. Need for cache operation with cache manager name or default cache manager.");
        }
        return cacheManager;
    }

    private String getDefinedName(Set<String> cacheNames, DefineCache defineCache) {
        String definedName = null;
        if (defineCache != null) {
            definedName = defineCache.name();
            if (StringUtils.isEmpty(definedName)) {
                if (cacheNames.size() > 1) {
                    throw new IllegalStateException("CacheableConfig does not has a name, but Cacheable is define multiple names.");
                }
                definedName = cacheNames.iterator().next();
            }
        }
        return definedName;
    }

    private CacheConfig createConfig(DefineCache defineCache, String cacheName) {
        return CacheConfig.builder()
          .expireAfterWrite(defineCache.expireAfterWrite())
          .name(cacheName)
          .build();
    }
}
