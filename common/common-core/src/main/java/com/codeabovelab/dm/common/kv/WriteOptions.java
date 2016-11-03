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
import org.springframework.util.Assert;

/**
 * Options of write operation. <p/>
 * Some engine my not implement all set of options.
 */
@Data
public class WriteOptions {

    @Data
    public static class Builder<T extends WriteOptions, B extends Builder<T, B>> {
        private int prevIndex = -1;
        private boolean failIfExists = false;
        private boolean failIfAbsent = false;
        /**
         * time to live in seconds.
         */
        private long ttl = -1;

        @SuppressWarnings("unchecked")
        protected B thiz() {
            return (B) this;
        }

        public B prevIndex(int prevIndex) {
            setPrevIndex(prevIndex);
            return thiz();
        }


        public B failIfExists(boolean failIfExists) {
            setFailIfExists(failIfExists);
            return thiz();
        }

        public B failIfAbsent(boolean failIfAbsent) {
            setFailIfAbsent(failIfAbsent);
            return thiz();
        }

        /**
         * time to live in seconds.
         * @param ttl
         * @return
         */
        public B ttl(long ttl) {
            setTtl(ttl);
            return thiz();
        }

        public WriteOptions build() {
            return new WriteOptions(this);
        }
    }

    private final int prevIndex;
    private final boolean failIfExists;
    private final boolean failIfAbsent;
    /**
     * time to live in seconds.
     */
    private final long ttl;

    public WriteOptions(Builder b) {
        this.prevIndex = b.prevIndex;
        this.failIfAbsent = b.failIfAbsent;
        this.failIfExists = b.failIfExists;
        Assert.isTrue(!this.failIfAbsent || !this.failIfExists, "Simultaneous set of both flags 'failIfAbsent'=" +
          this.failIfAbsent + " and 'failIfExists'=" + this.failIfExists + " to true is incorrect.");
        this.ttl = b.ttl;
    }

    public static Builder builder() {
        return new Builder();
    }
}
