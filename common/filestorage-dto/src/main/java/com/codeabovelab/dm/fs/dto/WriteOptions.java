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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.util.Assert;

/**
 * Options of write process
 */
public final class WriteOptions extends BasicOptions<WriteOptions> {


    public static final class Builder extends BasicOptions.Builder<Builder, WriteOptions> {
        private FileHandle fileHandle;

        public FileHandle getFileHandle() {
            return fileHandle;
        }

        public Builder fileHandle(FileHandle fileHandle) {
            setFileHandle(fileHandle);
            return this;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public void setFileHandle(FileHandle fileHandle) {
            this.fileHandle = fileHandle;
        }

        @Override
        public WriteOptions build() {
            return new WriteOptions(this);
        }

        public Builder from(WriteOptions options) {
            super.from(options);
            setFileHandle(options.getFileHandle());
            return this;
        }
    }

    private final FileHandle fileHandle;

    @JsonCreator
    public WriteOptions(Builder b) {
        super(b);
        this.fileHandle = b.fileHandle;
        Assert.notNull(this.fileHandle, "fileHandle is null");
    }

    public static WriteOptions.Builder builder() {
        return new Builder();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public FileHandle getFileHandle() {
        return fileHandle;
    }
}
