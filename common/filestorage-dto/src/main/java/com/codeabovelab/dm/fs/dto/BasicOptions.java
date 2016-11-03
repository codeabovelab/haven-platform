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

package com.codeabovelab.dm.fs.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.util.Assert;

/**
 * Options which customise behavior of FileStorage service.
 */
public class BasicOptions<T extends BasicOptions<T>> implements ExistsFlags {

    public static abstract class Builder<B extends Builder<B, T>, T extends BasicOptions<T>> {
        private boolean failIfExists;
        private boolean failIfAbsent;

        @SuppressWarnings({"unchecked"})
        protected B thiz() {
            return (B) this;
        }

        public B from(T options) {
            setFailIfAbsent(options.isFailIfAbsent());
            setFailIfExists(options.isFailIfExists());
            return thiz();
        }

        public boolean isFailIfExists() {
            return failIfExists;
        }

        public B failIfExists(boolean failIfExists) {
            setFailIfExists(failIfExists);
            return thiz();
        }

        public void setFailIfExists(boolean failIfExists) {
            this.failIfExists = failIfExists;
        }

        public boolean isFailIfAbsent() {
            return failIfAbsent;
        }

        public B failIfAbsent(boolean failIfAbsent) {
            setFailIfAbsent(failIfAbsent);
            return thiz();
        }

        public void setFailIfAbsent(boolean failIfAbsent) {
            this.failIfAbsent = failIfAbsent;
        }

        public abstract T build();
    }

    private final boolean failIfExists;
    private final boolean failIfAbsent;

    @JsonCreator
    public BasicOptions(Builder<?, ?> b) {
        Assert.isTrue(!b.failIfAbsent || !b.failIfExists, "Both b.failIfAbsent and b.failIfExists is true");
        this.failIfAbsent = b.failIfAbsent;
        this.failIfExists = b.failIfExists;
    }

    @Override
    public boolean isFailIfExists() {
        return failIfExists;
    }

    @Override
    public boolean isFailIfAbsent() {
        return failIfAbsent;
    }
}
