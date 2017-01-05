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

package com.codeabovelab.dm.common.kv;

import lombok.Data;

/**
 * DockerEvent of key value storage.
 */
@Data
public class KvStorageEvent {

    /**
     * Index of node
     */
    private final long index;
    private final String key;
    private final String value;
    private final long ttl;
    private final Crud action;
    public KvStorageEvent(long index, String key, String value, long ttl, Crud action) {
        this.index = index;
        this.key = key;
        this.value = value;
        this.ttl = ttl;
        this.action = action;
    }

    public enum Crud {
        CREATE, READ, UPDATE, DELETE
    }

}
