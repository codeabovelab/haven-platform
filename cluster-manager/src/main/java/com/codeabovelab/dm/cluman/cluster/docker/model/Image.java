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

import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * Represent Image inside Manifest DTO
 */
@EqualsAndHashCode
@Data
public class Image implements ImageDescriptor {

    private static final String CREATED = "Created";
    private static final String ID = "Id";
    private static final String PARENT = "Parent";
    private static final String CONTAINER_CONFIG = "ContainerConfig";
    private static final String REPO_TAGS = "RepoTags";
    private static final String SIZE = "Size";
    private static final String VIRTUAL_SIZE = "VirtualSize";

    @JsonProperty(CREATED)
    private final Date created;
    @JsonProperty(ID)
    private final String id;
    @JsonProperty(PARENT)
    private final String parent;
    @JsonProperty(REPO_TAGS)
    private final List<String> repoTags;
    @JsonProperty(CONTAINER_CONFIG)
    private final ContainerConfig containerConfig;
    @JsonProperty(SIZE)
    private final long size;
    @JsonProperty(VIRTUAL_SIZE)
    private final long virtualSize;

    @Builder
    public Image(@JsonProperty(CONTAINER_CONFIG) ContainerConfig containerConfig,
                 @JsonProperty(PARENT) String parent,
                 @JsonProperty(ID) String id,
                 @JsonProperty(CREATED) Date created,
                 @JsonProperty(REPO_TAGS) List<String> repoTags,
                 @JsonProperty(SIZE) long size,
                 @JsonProperty(VIRTUAL_SIZE) long virtualSize
    ) {
        this.containerConfig = containerConfig;
        this.parent = parent;
        this.id = id;
        this.created = created;
        this.repoTags = repoTags == null? ImmutableList.of() : ImmutableList.copyOf(repoTags);
        this.size = size;
        this.virtualSize = virtualSize;
    }


    @Override
    public String toString() {
        return "Image{" +
                "created=" + created +
                ", id='" + id + '\'' +
                ", parent='" + parent + '\'' +
                ", containerConfig=" + containerConfig +
                '}';
    }
}
