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

package com.codeabovelab.dm.cluman.cluster.registry.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;

/**
 * Image manifest
 */
@Value
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class Manifest {

    @Value
    @AllArgsConstructor(onConstructor = @__(@JsonCreator))
    public static class Entry {
        private final String mediaType;
        private final long size;
        private final String digest;
    }

    private final int schemaVersion;
    private final String mediaType;
    private final Entry config;
    private final List<Entry> layers;
}
