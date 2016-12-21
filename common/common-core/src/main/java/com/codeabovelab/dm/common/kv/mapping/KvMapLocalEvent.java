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
import lombok.Data;

/**
 * Like {@link KvMapEvent }, but caused by local modifications, or processing of remote changes on local map.
 * For example, deletion key in KV cause remote events, and local event too if local map has value for this key.
 */
@Data
public class KvMapLocalEvent<T> {
    public enum Action {
        /**
         * save new value
         */
        CREATE,
        /**
         * save value over existed
         */
        UPDATE,
        /**
         * Delete existed value
         */
        DELETE,
        /**
         * Load existed value
         */
        LOAD
    }

    private final KvMap<T> map;
    private final Action action;
    private final String key;
    private final T oldValue;
    private final T newValue;
}
