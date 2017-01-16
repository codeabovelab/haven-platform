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
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Base class for some DTOs.<p/>
 * See: https://github.com/docker/docker/blob/master/api/types/swarm/common.go#L11 <p/>
 * Note that it mutable version, we can not make immutable kind of this because lombok & jackson has some issues
 * which prevent it.
 */
@Data
public class MetaMutable {
    @JsonProperty("ID")
    private String id;
    @JsonProperty("Version")
    private SwarmVersion version;
    @JsonProperty("CreatedAt")
    private LocalDateTime created;
    @JsonProperty("UpdatedAt")
    private LocalDateTime updated;
}
