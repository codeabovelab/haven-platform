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


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;


/**
 * Representation of a Docker statistics.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class Statistics {

    @JsonProperty("read")
    private String read;

    /**
     * @since Docker Remote API 1.21
     */
    @JsonProperty("networks")
    private Map<String, Object> networks;

    /**
     * @deprecated as of Docker Remote API 1.21, replaced by {@link #networks}
     */
    @Deprecated
    @JsonProperty("network")
    private Map<String, Object> network;

    @JsonProperty("memory_stats")
    private Map<String, Object> memoryStats;

    @JsonProperty("blkio_stats")
    private Map<String, Object> blkioStats;

    @JsonProperty("cpu_stats")
    private Map<String, Object> cpuStats;

    @JsonProperty("precpu_stats")
    private Map<String, Object> precpuStats;

}