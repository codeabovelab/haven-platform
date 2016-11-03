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

import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;

import java.util.*;

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

    public UiImageCatalog(String name) {
        this.name = name;
        this.registry = ContainerUtils.isImageId(this.name)? null : ContainerUtils.getRegistryName(this.name);
    }

    @JsonProperty
    public Collection<UiImageData> getIds() {
        ArrayList<UiImageData> list = new ArrayList<>(ids.values());
        list.sort((l, r) -> {
            Date lc = l.getCreated();
            Date rc = r.getCreated();
            if(lc == null || rc == null) {
                return lc != null? 1 : (rc != null? -1 : 0);
            }
            return lc.compareTo(rc);
        });
        return list;
    }

    public UiImageData getOrAddId(String id) {
        return ids.computeIfAbsent(id, UiImageData::new);
    }

    @Override
    public int compareTo(UiImageCatalog o) {
        return name.compareTo(o.name);
    }
}
