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

package com.codeabovelab.dm.cluman.cluster.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 */
@Data
public class ImageItem {

    private static final String CREATED = "Created";
    private static final String ID = "Id";
    private static final String REPO_TAGS = "RepoTags";
    private static final String SIZE = "Size";
    private static final String VIRTUAL_SIZE = "VirtualSize";
    private static final String LABELS = "Labels";

    private final Date created;
    private final String id;
    private final List<String> repoTags;
    private final long size;
    private final long virtualSize;
    private final Map<String, String> labels;

    @Builder
    public ImageItem(@JsonProperty(ID) String id,
                     @JsonProperty(CREATED) long created,
                     @JsonProperty(REPO_TAGS) List<String> repoTags,
                     @JsonProperty(SIZE) long size,
                     @JsonProperty(VIRTUAL_SIZE) long virtualSize,
                     @JsonProperty(LABELS) Map<String, String> labels
    ) {
        this.id = id;
        // docker's date is in seconds
        this.created = new Date(created * 1000L);
        this.repoTags = repoTags == null? ImmutableList.of() : ImmutableList.copyOf(repoTags);
        this.size = size;
        this.virtualSize = virtualSize;
        this.labels = labels == null? ImmutableMap.of() : ImmutableMap.copyOf(labels);
    }
}

