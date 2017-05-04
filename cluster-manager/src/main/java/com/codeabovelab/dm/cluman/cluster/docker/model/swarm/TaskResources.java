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

package com.codeabovelab.dm.cluman.cluster.docker.model.swarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * https://github.com/docker/docker/blob/a5da9f5cc911da603a41bb77ca1ccbb0848d6260/api/types/swarm/task.go#L70
 */
@Data
@AllArgsConstructor
@Builder(builderClassName = "Builder")
public class TaskResources {

    /**
     * CPU limit in units of 10^-9 CPU shares.
     */
    @JsonProperty("NanoCPUs")
    private final long nanoCPUs;

    @JsonProperty("MemoryBytes")
    private final long memory;
}
