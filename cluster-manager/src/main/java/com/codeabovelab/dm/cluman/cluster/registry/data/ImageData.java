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

import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 */
@Data
public class ImageData {
    private String architecture;
    private String author;
    private ContainerConfig config;
    @JsonProperty("container_config")
    private ContainerConfig containerConfig;
    private Date created;
    @JsonProperty("docker_version")
    private String dockerVersion;
    private String os;
    // we do not need this yet
    //private List<HistoryEntry> history;

}
