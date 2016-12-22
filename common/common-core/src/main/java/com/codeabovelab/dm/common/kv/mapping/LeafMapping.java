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

import com.codeabovelab.dm.common.kv.KvNode;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 */
class LeafMapping<T> extends AbstractMapping<T> {
    LeafMapping(KvMapperFactory mapper, Class<T> type) {
        super(mapper, type);
    }

    @Override
    void save(String path, T object, KvSaveCallback callback) {
        try {
            String value = getObjectMapper().writeValueAsString(object);
            KvNode res = getStorage().set(path, value);
            if(callback != null) {
                callback.call(null, res);
            }
        } catch (Exception e) {
            throw new RuntimeException("Can not save object at path: " + path, e);
        }
    }

    @Override
    void load(String path, T object) {
        KvNode node = getStorage().get(path);
        if(node == null) {
            return;
        }
        String str = node.getValue();
        try {
            ObjectReader reader = getObjectMapper().reader();
            reader.withValueToUpdate(object).readValue(str);
        } catch (Exception e) {
            throw new RuntimeException("Can not save object at path: " + path, e);
        }
    }

    @Override
    <S extends T> S load(String path, String name, Class<S> type) {
        KvNode node = getStorage().get(path);
        if(node == null) {
            return null;
        }
        String str = node.getValue();
        try {
            return getObjectMapper().readValue(str, type);
        } catch (Exception e) {
            throw new RuntimeException("Can not save object at path: " + path, e);
        }
    }
}
