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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Information about tags.
 */
@Data
public class Tag {

    private static final String NAME = "name"; // Name of the target repository.
    private static final String LAYER = "layer"; // ID of layer

    private final String name;
    private final String layer;

    public Tag(@JsonProperty(NAME) String name,
               @JsonProperty(LAYER) String layer) {
        this.name = name;
        this.layer = layer;
    }

    @JsonProperty(NAME)
    public String getName() {
        return name;
    }

    @JsonProperty(LAYER)
    public String getLayer() {
        return layer;
    }

}
