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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
@Builder
public class Image implements ImageDescriptor {


    @JsonProperty("Created")
    private final Date created;
    @JsonProperty("Id")
    private final String id;
    @JsonProperty("Parent")
    private final String parent;
    @JsonProperty("RepoTags")
    private final List<String> repoTags;
    @JsonProperty("ContainerConfig")
    private final ContainerConfig containerConfig;
    @JsonProperty("Size")
    private final long size;
    @JsonProperty("VirtualSize")
    private final long virtualSize;

}
