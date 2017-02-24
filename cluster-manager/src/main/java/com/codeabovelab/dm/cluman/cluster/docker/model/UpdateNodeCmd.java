/*
 * Copyright 2017 Code Above Lab LLC
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

import com.codeabovelab.dm.common.json.JtEnumLower;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 */
@Data
public class UpdateNodeCmd {
    /**
     * Node id from swarm mode of docker. <p/>
     * Not confuse it with node name or address, it must be string like '24ifsmvkjbyhk'.
     */
    @JsonIgnore
    private String nodeId;

    @JsonIgnore
    private long version;

    /**
     * Name for the node.
     */
    @JsonProperty("Name")
    private String name;

    /**
     * User-defined key/value metadata.
     */
    @JsonProperty("Labels")
    private Map<String, String> labels;

    /**
     * Role of the node.
     */
    @JsonProperty("Role")
    private Role role;

    /**
     * Availability of the node.
     */
    @JsonProperty("Availability")
    private Availability availability;

    @JtEnumLower
    public enum Role {
        WORKER, MANAGER
    }

    @JtEnumLower
    public enum Availability {
        ACTIVE, PAUSE, DRAIN
    }
}
