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
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * https://github.com/docker/docker/blob/a69c4129e086e4e7b86cce7d2682685dfdc6f2d2/api/types/types.go#L418
 */
@Data
public class CreateNetworkCmd {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("CheckDuplicate")
    private boolean checkDuplicate;

    @JsonProperty("EnableIPv6")
    private boolean enableIpv6;

    @JsonProperty("Driver")
    private String driver;

    @JsonProperty("Internal")
    private boolean internal;

    @JsonProperty("Attachable")
    private boolean attachable;

    @JsonProperty("IPAM")
    private Network.Ipam ipam;

    @JsonProperty("Options")
    private final Map<String, String> options = new HashMap<>();

    @JsonProperty("Labels")
    private final Map<String, String> labels = new HashMap<>();
}
