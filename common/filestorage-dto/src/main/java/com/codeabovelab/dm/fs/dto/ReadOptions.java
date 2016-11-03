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
 * Options of read process.
 */
public final class ReadOptions extends BasicOptions<ReadOptions> {

    public static final class Builder extends BasicOptions.Builder<Builder, ReadOptions> {

        private String id;

        /**
         * UUID of file
         * @return
         */
        public String getId() {
            return id;
        }

        /**
         * UUID of file
         * @param id
         * @return
         */
        public Builder id(String id) {
            setId(id);
            return this;
        }

        /**
         * UUID of file
         * @param id
         */
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public ReadOptions build() {
            return new ReadOptions(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String id;

    @JsonCreator
    public ReadOptions(Builder b) {
        super(b);
        this.id = b.id;
        Assert.hasText(b.id, "is is null or empty");
    }

    /**
     * UUID of file
     * @return
     */
    public String getId() {
        return id;
    }
}
