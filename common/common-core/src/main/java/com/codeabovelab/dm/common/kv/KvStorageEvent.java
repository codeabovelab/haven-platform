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

/**
 * DockerEvent of key value storage.
 */
public class KvStorageEvent {

    public enum Crud {
        CREATE, READ, UPDATE, DELETE
    }

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

    public long getIndex() {
        return index;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public long getTtl() {
        return ttl;
    }

    public Crud getAction() {
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KvStorageEvent)) {
            return false;
        }

        KvStorageEvent that = (KvStorageEvent) o;

        if (ttl != that.ttl) {
            return false;
        }
        if (key != null ? !key.equals(that.key) : that.key != null) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }
        return action == that.action;

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (int) (ttl ^ (ttl >>> 32));
        result = 31 * result + (action != null ? action.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "KVE[" + index + "]{" +
          "'" + key + "\'=\'" + value + '\'' +
          ", ttl=" + ttl +
          ", action=" + action +
          '}';
    }
}
