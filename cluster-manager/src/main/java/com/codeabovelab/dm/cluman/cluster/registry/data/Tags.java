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

import java.util.List;

/**
 * Information about tags.
 */
public class Tags {

    private static final String NAME = "name"; // Name of the target repository.
    private static final String TAGS = "tags"; // A list of tags for the named repository.

    private final String name;
    private final List<String> tags;

    public Tags(@JsonProperty(NAME) String name,
                @JsonProperty(TAGS) List<String> tags) {
        this.name = name;
        this.tags = tags;
    }

    @JsonProperty(NAME)
    public String getName() {
        return name;
    }

    @JsonProperty(TAGS)
    public List<String> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return "Tags{" +
                "name='" + name + '\'' +
                ", tags=" + tags +
                '}';
    }
}
