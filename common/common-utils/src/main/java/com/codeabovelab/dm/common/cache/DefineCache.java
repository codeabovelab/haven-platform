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

import java.lang.annotation.*;

/**
 * Annotation for define cache.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface DefineCache {
    /**
     * Name of cache which is configured by this annotation. If name is absent, then used default cache name.
     * If {@link org.springframework.cache.annotation.Cacheable } define multiple caches and name is
     * absent then exception will be raised.
     * @return
     */
    String name() default "";

    /**
     * Name of cache manager bean
     * @return
     */
    String cacheManager() default "";

    /**
     * Timeout after last writing, after that cached value is expired.
     * Zero value mean that cache never expired, negative value - that cache managed must use default value.
     * @return
     */
    long expireAfterWrite() default -1;

    /**
     * Class of cache invalidator
     * @return
     */
    Class<? extends CacheInvalidator> invalidator() default CacheInvalidator.NullInvalidator.class;

    /**
     * Arguments of cache invalidator. Sequence of key value pairs, like {key1, val1, key2, val2,...}.
     * @return
     */
    String[] invalidatorArgs() default {};
}
