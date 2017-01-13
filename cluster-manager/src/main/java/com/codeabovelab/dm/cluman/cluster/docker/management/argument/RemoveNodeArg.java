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

package com.codeabovelab.dm.cluman.cluster.docker.management.argument;

import lombok.Data;

/**
 */
@Data
public class RemoveNodeArg {
    private Boolean force;
    /**
     * Node id from swarm mode of docker. <p/>
     * Not confuse it with node name or address, it must be string like '24ifsmvkjbyhk'.
     */
    private String nodeId;

    public RemoveNodeArg() {

    }

    public RemoveNodeArg(String nodeId) {
        setNodeId(nodeId);
    }

    public RemoveNodeArg force(Boolean force) {
        setForce(force);
        return this;
    }
}
