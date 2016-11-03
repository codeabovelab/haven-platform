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
import lombok.EqualsAndHashCode;

/**
 * Options of write operation. <p/>
 * Some engine my not implement all set of options.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeleteDirOptions extends WriteOptions {

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Builder extends WriteOptions.Builder<DeleteDirOptions, Builder> {
        /**
         * Make operation for all underline items. Usual it allow remove non empty dir.
         */
        private boolean recursive;

        /**
         * Make operation for all underline items. Usual it allow remove non empty dir.
         * @param recursive
         * @return
         */
        public Builder recursive(boolean recursive) {
            setRecursive(recursive);
            return this;
        }

        @Override
        public DeleteDirOptions build() {
            return new DeleteDirOptions(this);
        }
    }

    /**
     * Make operation for all underline items. Usual it allow remove non empty dir.
     */
    private final boolean recursive;

    public static Builder builder() {
        return new Builder();
    }

    public DeleteDirOptions(Builder b) {
        super(b);
        this.recursive = b.recursive;
    }
}
