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

package com.codeabovelab.dm.common.fc;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 */
public class FbJacksonAdapter<T> implements FbAdapter<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> type;

    @Data
    private static class Wrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        private Object object;
    }

    public FbJacksonAdapter(ObjectMapper objectMapper, Class<T> type) {
        this.objectMapper = objectMapper;
        this.type = type;
        Assert.notNull(this.objectMapper, "objectMapper is null");
        Assert.notNull(this.type, "type is null");
    }

    @Override
    public byte[] serialize(T obj) throws IOException {
        type.cast(obj);
        Wrapper wrapper = new Wrapper();
        wrapper.setObject(obj);
        return objectMapper.writeValueAsBytes(wrapper);
    }

    @Override
    public T deserialize(byte[] data, int offset, int len) throws IOException {
        Wrapper wrapper = objectMapper.readValue(data, offset, len, Wrapper.class);
        return type.cast(wrapper.getObject());
    }
}
