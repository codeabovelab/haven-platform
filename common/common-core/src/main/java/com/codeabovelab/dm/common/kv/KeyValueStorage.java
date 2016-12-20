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

import com.codeabovelab.dm.common.mb.ConditionalSubscriptions;

import java.util.List;
import java.util.Map;

/**
 * key-value store for shared configuration and service discovery
 */
public interface KeyValueStorage {

    /**
     * Get the value of a key.
     * @param key the key
     * @return the node with corresponding value or null when not found value
     */
    KvNode get(String key);

    /**
     * Setting the value of a key
     * @param key the key
     * @param value the value
     * @return node
     */
    default KvNode set(String key, String value) {
        return set(key, value, null);
    }

    /**
     * Setting the value of a key with options
     * @param key the key
     * @param value the value
     * @param ops ops or null
     * @return node
     */
    KvNode set(String key, String value, WriteOptions ops);

    /**
     * Make or update directory at specified key.
     * @param key
     * @param ops ops or null
     * @throws Exception
     * @return node
     */
    KvNode setdir(String key, WriteOptions ops);

    /**
     * Delete directory
     * @param key
     * @param ops
     * @throws Exception
     * @return node
     */
    KvNode deletedir(String key, DeleteDirOptions ops);


    /**
     * Delete a key
     * @param key the key
     * @param ops ops or null
     * @return node
     */
    KvNode delete(String key, WriteOptions ops);

    /**
     * List keys of specified prefix
     * @param key
     * @return list or null if key is absent
     */
    List<String> list(String key);

    /**
     * Retrieve map of keys and its values from specified prefix.
     * @param key
     * @return map or null if key is absent
     */
    Map<String, String> map(String key);

    /**
     * Subscriptions for key value events of this storage. <p/>
     * Note that subscription may be on '/key' - or on key with its childs '/key*' (also '/key/*')
     * @return
     */
    ConditionalSubscriptions<KvStorageEvent, String> subscriptions();

    String getPrefix();
}
