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

package com.codeabovelab.dm.cluman.reconfig;

import com.codeabovelab.dm.common.kv.KvSupportModule;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.json.CustomTypeIdResolver;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Entry point for managing of Application Configuration.
 */
@Component
public class AppConfigService {

    private static final String VERSION = "1.0";
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, ReConfigurableAdapter> adapters = new ConcurrentHashMap<>();

    @Autowired
    public AppConfigService(KvMapperFactory kvMapperFactory, ObjectMapper objectMapper) {
        // we must use custom configuration of mapper, but need use most options and modules from global mapper
        this.objectMapper = objectMapper.copy();
        StdTypeResolverBuilder trb = new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
        trb.init(JsonTypeInfo.Id.CLASS, new CustomTypeIdResolver()).inclusion(JsonTypeInfo.As.PROPERTY);
        this.objectMapper.setDefaultTyping(trb);
        this.objectMapper.registerModule(new KvSupportModule(kvMapperFactory));
    }

    void registerIfAbsent(String name, Supplier<ReConfigurableAdapter> factory) {
        Assert.hasText(name, "name is null or empty");
        adapters.computeIfAbsent(name, s -> factory.get());
    }

    /**
     */
    public void write(String mimeType, OutputStream os) throws IOException {
        Assert.hasText(mimeType, "MimeType string is null or empty.");
        Assert.notNull(os, "OutputStream is null or empty.");
        MimeType mimeTypeObj = MimeTypeUtils.parseMimeType(mimeType);
        if(MimeTypeUtils.APPLICATION_JSON.equals(mimeTypeObj)) {
            Assert.hasText(mimeType, "MimeType '" + mimeType + "' is not supported.");
        }
        AppConfigObject aco = new AppConfigObject();
        aco.setDate(LocalDateTime.now());
        aco.setVersion(VERSION);
        Map<String, Object> map = new HashMap<>();
        aco.setData(map);
        ConfigWriteContext ctx = ConfigWriteContext.builder()
          .mimeType(mimeTypeObj)
          .build();
        for(ConcurrentMap.Entry<String, ReConfigurableAdapter> cae : adapters.entrySet()) {
            ReConfigurableAdapter ca = cae.getValue();
            Object o = ca.getConfig(ctx);
            if(o == null) {
                continue;
            }
            String name = cae.getKey();
            map.put(name, o);
        }
        objectMapper.writeValue(os, aco);
    }

    public void read(String mimeType, InputStream is) throws IOException {
        Assert.hasText(mimeType, "MimeType string is null or empty.");
        Assert.notNull(is, "InputStream is null or empty.");
        MimeType mimeTypeObj = MimeTypeUtils.parseMimeType(mimeType);
        if(MimeTypeUtils.APPLICATION_JSON.equals(mimeTypeObj)) {
            Assert.hasText(mimeType, "MimeType '" + mimeType + "' is not supported.");
        }
        AppConfigObject aco = objectMapper.readValue(is, AppConfigObject.class);
        final String version = aco.getVersion();
        if(!VERSION.equals(version)) {
            throw new RuntimeException("Unsupported version of config: " + version);
        }

        ConfigReadContext ctx = new ConfigReadContext();
        Map<String, Object> map = aco.getData();
        Assert.notNull(map, "config has empty map");
        for(Map.Entry<String, Object> oe : map.entrySet()) {
            String name = oe.getKey();
            ReConfigurableAdapter ca = adapters.get(name);
            Assert.notNull(ca, "Can not find adapter with name: " + name);
            Object value = oe.getValue();
            Assert.notNull(value, "Config object is null for name: " + name);
            ca.setConfig(ctx, value);
        }
    }
}
