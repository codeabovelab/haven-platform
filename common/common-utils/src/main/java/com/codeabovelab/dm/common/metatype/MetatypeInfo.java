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

package com.codeabovelab.dm.common.metatype;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation which describe the MetaType.
 * This annotation can be convert to {@link com.codeabovelab.dm.common.utils.Key } by {@link MetatypeUtils#toKey(MetatypeInfo)}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MetatypeInfo {
    /**
     * Name of metatype.
     * @return
     */
    String name();

    /**
     * Type of metatype.
     * @return
     */
    Class<?> type();
}
