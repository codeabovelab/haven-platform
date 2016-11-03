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

import java.io.Serializable;

/**
 * Immutable config for cache
 */
public final class CacheConfig implements Serializable, CacheConfigSource {

    public static class Builder implements CacheConfigSource {
        private String name;
        private long expireAfterWrite = -1;

        public Builder from(CacheConfigSource config) {
            if(config != null) {
                setName(config.getName());
                setExpireAfterWrite(config.getExpireAfterWrite());
            }
            return this;
        }

        /**
         * name of cache
         * @return
         */
        public String getName() {
            return name;
        }

        /**
         * name of cache
         * @param name
         * @return
         */
        public Builder name(String name) {
            setName(name);
            return this;
        }

        /**
         * name of cache
         * @param name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Timeout after last writing, after that cached value is expired.
         * Zero value mean that cache never expired, negative value - that cache managed must use default value.
         * @return
         */
        public long getExpireAfterWrite() {
            return expireAfterWrite;
        }

        /**
         * Timeout after last writing, after that cached value is expired.
         * Zero value mean that cache never expired, negative value - that cache managed must use default value.
         * @param timeout
         * @return
         */
        public Builder expireAfterWrite(long timeout) {
            setExpireAfterWrite(timeout);
            return this;
        }

        /**
         * Timeout after last writing, after that cached value is expired.
         * Zero value mean that cache never expired, negative value - that cache managed must use default value.
         * @param expireAfterWrite
         */
        public void setExpireAfterWrite(long expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }

        public CacheConfig build() {
            return new CacheConfig(this);
        }
    }

    private final String name;
    private final long expireAfterWrite;

    public CacheConfig(CacheConfigSource builder) {
        this.name = builder.getName();
        this.expireAfterWrite = builder.getExpireAfterWrite();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * name of cache
     * @return
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Timeout after last writing, after that cached value is expired.
     * Zero value mean that cache never expired, negative value - that cache managed must use default value.
     * @return
     */
    @Override
    public long getExpireAfterWrite() {
        return expireAfterWrite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CacheConfig)) {
            return false;
        }

        CacheConfig that = (CacheConfig) o;

        if (expireAfterWrite != that.expireAfterWrite) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (int) (expireAfterWrite ^ (expireAfterWrite >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "CacheConfig{" +
          "name='" + name + '\'' +
          ", expireAfterWrite=" + expireAfterWrite +
          '}';
    }
}
