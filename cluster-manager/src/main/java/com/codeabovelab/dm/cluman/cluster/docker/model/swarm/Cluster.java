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

package com.codeabovelab.dm.cluman.cluster.docker.model.swarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Base class for cluster info in 'GET /swarm' (this use subclass with tokens) and 'GET /info' requests. <p/>
 * <pre>
   {
     "ID":"6r...lt",
     "Version":{"Index":11},
     "CreatedAt":"2016-12-29T15:26:15.372810703Z",
     "UpdatedAt":"2016-12-29T15:26:15.474602597Z",
     "Spec": // see {@link SwarmSpec}
   }
 * </pre>
 */
@Data
public class Cluster {
    @JsonProperty("ID")
    private String id;
    @JsonProperty("Version")
    private SwarmVersion version;
    @JsonProperty("CreatedAt")
    private LocalDateTime created;
    @JsonProperty("UpdatedAt")
    private LocalDateTime updated;
    @JsonProperty("Spec")
    private SwarmSpec spec;

}
