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
 * Options of delete process
 */
public final class DeleteOptions extends BasicOptions<DeleteOptions> {

    public static final class Builder extends BasicOptions.Builder<Builder, DeleteOptions> {

        private String id;

        public String getId() {
            return id;
        }

        public Builder id(String id) {
            setId(id);
            return this;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public Builder from(DeleteOptions options) {
            super.from(options);
            setId(options.getId());
            return this;
        }

        @Override
        public DeleteOptions build() {
            return new DeleteOptions(this);
        }
    }

    private final String id;

    @JsonCreator
    public DeleteOptions(Builder b) {
        super(b);
        this.id = b.getId();
        Assert.hasText(id, "id is null or empty");
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }
}
