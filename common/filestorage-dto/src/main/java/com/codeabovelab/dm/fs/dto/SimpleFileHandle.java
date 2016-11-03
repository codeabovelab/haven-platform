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

import com.codeabovelab.dm.common.utils.Throwables;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

/**
 * Simple immutable implementation of file handle.
 */
public final class SimpleFileHandle implements FileHandle {
    public static class Builder {
        private String id;
        private final Map<String, String> attributes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private Callable<InputStream> streamFactory;

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

        public Builder putAttribute(String key, String value) {
            this.attributes.put(key, value);
            return this;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public Builder attributes(Map<String, String> attributes) {
            setAttributes(attributes);
            return this;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes.clear();
            if(attributes != null) {
                this.attributes.putAll(attributes);
            }
        }

        public Callable<InputStream> getStreamFactory() {
            return streamFactory;
        }

        public Builder streamFactory(Callable<InputStream> streamFactory) {
            setStreamFactory(streamFactory);
            return this;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public void setStreamFactory(Callable<InputStream> streamFactory) {
            this.streamFactory = streamFactory;
        }

        public SimpleFileHandle build() {
            return new SimpleFileHandle(this);
        }
    }

    private final String id;
    private final Map<String, String> attributes;
    private final Callable<InputStream> streamFactory;

    @JsonCreator
    public SimpleFileHandle(Builder b) {
        this.streamFactory = b.streamFactory;
        this.attributes = FileStorageUtils.unmodifiableCaseInsensitiveCopy(b.attributes);
        this.id = b.id;
        Assert.hasText(id, "id is null or empty");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public InputStream getData() {
        if(streamFactory == null) {
            return null;
        }
        try {
            return this.streamFactory.call();
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public Callable<InputStream> getStreamFactory() {
        return streamFactory;
    }

    @Override
    public Map<String, String> getAttributes() {
        return this.attributes;
    }
}
