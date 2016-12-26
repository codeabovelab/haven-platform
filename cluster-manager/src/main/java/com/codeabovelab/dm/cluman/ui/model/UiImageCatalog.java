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

package com.codeabovelab.dm.cluman.ui.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.reverseOrder;

/**
 */
@Data
public class UiImageCatalog implements Comparable<UiImageCatalog> {
    private final String name;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private final String registry;
    @JsonIgnore
    @lombok.Getter(value = AccessLevel.NONE)
    private final Map<String, UiImageData> ids = new TreeMap<>();
    private final Set<String> clusters = new TreeSet<>();

    public UiImageCatalog(String name, String registry) {
        this.name = name;
        this.registry = registry;
    }

    @JsonProperty
    public List<UiImageData> getIds() {
        return ids.values().stream().sorted(reverseOrder(UiImageData::compareTo)).collect(Collectors.toList());
    }

    public UiImageData getOrAddId(String id) {
        return ids.computeIfAbsent(id, UiImageData::new);
    }

    @Override
    public int compareTo(UiImageCatalog o) {
        return name.compareTo(o.name);
    }
}
