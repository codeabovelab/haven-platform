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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * https://github.com/docker/docker/blob/a69c4129e086e4e7b86cce7d2682685dfdc6f2d2/api/types/types.go#L442
 */
@Data
public class ConnectNetworkCmd {

    /**
     * Name or id of network
     */
    @JsonIgnore
    private String network;

    /**
     * name or id of container
     */
    @JsonProperty("Container")
    private String container;

    /**
     * Configuration for a network endpoint.
     */
    @JsonProperty("EndpointConfig")
    private EndpointSettings config;
}
