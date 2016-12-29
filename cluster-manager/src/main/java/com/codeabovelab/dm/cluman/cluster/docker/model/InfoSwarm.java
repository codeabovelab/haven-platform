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

import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Cluster;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.SwarmSpec;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Swarm part of {@link Info } . <p/>
 * <pre>
   {
     "NodeID":"2q...5g",
     "NodeAddr":"172.31.0.12",
     "LocalNodeState":"active",
     "ControlAvailable":true,
     "Error":"",
     "RemoteManagers":[
       {"NodeID":"2q...5g","Addr":"172.31.0.12:4375"}
     ],
     "Nodes":1,
     "Managers":1,
     "Cluster"://see {@link Cluster }
    }
 * </pre>
 */
@Data
public class InfoSwarm {
    @JsonProperty("NodeID")
    private String nodeId;
    @JsonProperty("NodeAddr")
    private String address;
    @JsonProperty("LocalNodeState")
    private String localNodeState;
    /**
     * Internal it meant that this node a manager:
     * <pre>" Is Manager: %v\n", info.Swarm.ControlAvailable)</pre>
     * https://github.com/docker/docker/blob/b9ee31ae027bbd62477fea3f58023c90f051db00/cli/command/system/info.go#L105
     */
    @JsonProperty("ControlAvailable")
    private boolean controlAvailable;
    @JsonProperty("Error")
    private String error;
    @JsonProperty("RemoteManagers")
    private List<Manager> remoteManagers;
    @JsonProperty("Nodes")
    private int nodes;
    @JsonProperty("Managers")
    private int managers;
    @JsonProperty("Cluster")
    private Cluster cluster;

    @Data
    public static class Manager {
        /**
         * node id
         */
        @JsonProperty("NodeID")
        private String nodeId;
        @JsonProperty("Addr")
        private String address;
    }
}
