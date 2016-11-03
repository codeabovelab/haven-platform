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

package com.codeabovelab.dm.common.kv.mapping;

import com.codeabovelab.dm.common.kv.KeyValueStorage;
import com.codeabovelab.dm.common.kv.KvUtils;
import com.codeabovelab.dm.common.validate.Validity;
import com.codeabovelab.dm.common.utils.Throwables;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

/**
 * Helper which enclose middle code between object and its kv node. <p/>
 * Current behaviour:
 * <ul>
 *     <li>replace field values in object at {@link #load()}</li>
 *     <li>put field values from object to storage at {@link #save()}</li>
 * </ul>
 */
public class KvMapper<T> {

    private final Map<String, KvPropertyState> props;
    private final KvMapperFactory factory;
    private final KeyValueStorage storage;
    private final String prefix;
    private boolean internalSet = false;
    private volatile int ttl = 0;
    private final T object;

    KvMapper(KvMapperFactory factory, T object, String prefix) {
        this.factory = factory;
        this.object = object;
        Assert.notNull(object, "object is null");
        this.storage = this.factory.getStorage();
        this.prefix = prefix;
        Assert.hasText(prefix, "prefix is null");

        this.props = this.factory.loadProps(object.getClass(), KvPropertyState::new);
    }

    /**
     * Load node from storage
     */
    public void load() {
        try {
            Map<String, String> map = this.storage.map(this.prefix);
            loadInternal(map);
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }
    }

    public Validity validate() {
        return factory.validate(this.prefix, this.object);
    }

    private void loadInternal(Map<String, String> map) {
        if(map == null) {
            return;
        }
        internalSet = true;
        try {
            for (Map.Entry<String, String> e: map.entrySet()) {
                String propName = KvUtils.suffix(this.prefix, e.getKey());
                KvPropertyState propState = this.props.get(propName);
                if(propState == null) {
                    continue;
                }
                KvProperty property = propState.getProperty();
                String str = e.getValue();
                property.set(this.object, str);
                propState.setModified(false);
            }
        } finally {
            internalSet = false;
        }
    }

    /**
     * Save modified node data into storage
     */
    public void save() {
        for(KvPropertyState propState: this.props.values()) {
            if(!propState.isModified()) {
                continue;
            }
            KvProperty property = propState.getProperty();
            String strval = property.get(this.object);
            String path = KvUtils.join(this.prefix, property.getKey());
            try {
                this.storage.set(path, strval);
            } catch (Exception e) {
                throw new RuntimeException("Error at path: " + path, e);
            }
            propState.setModified(false);
        }
    }



    /**
     * Immediately create node in storage
     */
    public void create() {
        //this is workaround for creating node directory without any attributes,
        // in some KV we can create dirs, in other - not, therefore we need to make some key value record
        try {
            storage.set(KvUtils.join(prefix, "_created"), DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()));
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }
    }

    public void loadOrCreate() {
        try {
            Map<String, String> map = this.storage.map(this.prefix);
            if(map == null) {
                create();
            } else {
                loadInternal(map);
            }
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }
    }

    public int getTtl() {
        return ttl;
    }

    /**
     * ttl in seconds
     * @param ttl
     */
    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    /**
     * Below method must be called before changing properties of mapped object.
     * @param key
     * @param oldVal
     * @param newVal
     */
    public void onSet(String key, Object oldVal, Object newVal) {
        if(Objects.equals(oldVal, newVal) || internalSet) {
            return;
        }
        modify(key);
    }

    public void modify(String key) {
        KvPropertyState state = props.get(key);
        Assert.notNull(state, "Unknown key: " + key);
        state.setModified(true);
    }

}
