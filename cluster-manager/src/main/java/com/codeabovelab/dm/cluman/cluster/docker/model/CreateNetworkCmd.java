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
 * TODO It must consider all fields from {@link Network }
 */
@Data
public class CreateNetworkCmd {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("CheckDuplicate")
    private boolean checkDuplicate;

    @JsonProperty("Driver")
    private String driver;

    @JsonProperty("Internal")
    private boolean internal;

    @JsonProperty("IPAM")
    private Network.Ipam ipam;

    @JsonProperty("Options")
    private final Map<String, String> options = new HashMap<>();

    @JsonProperty("Labels")
    private final Map<String, String> labels = new HashMap<>();
}
