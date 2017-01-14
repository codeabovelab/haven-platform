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

import java.util.ArrayList;
import java.util.List;

/**
     {
         "ListenAddr": "0.0.0.0:2377",
         "AdvertiseAddr": "192.168.1.1:2377",
         "RemoteAddrs": ["node1:2377"],
         "JoinToken": "SWMTKN-1-3pu6hszjas19xyp7ghgosyx9k8atbfcr8p2is99znpy26u2lkl-7p73s1dx5in4tatdymyhg9hu2"
     }
 */
@Data
public class SwarmJoinCmd {
    /**
     * 'Listen address used for inter-manager communication if the node gets promoted to manager, as well as
     * determining the networking interface used for the VXLAN Tunnel Endpoint (VTEP).'
     */
    @JsonProperty("ListenAddr")
    private String listen;

    /**
     * 'Externally reachable address advertised to other nodes.
     * If AdvertiseAddr is not specified, it will be automatically detected when possible.'
     */
    @JsonProperty("AdvertiseAddr")
    private String advertise;

    /**
     * 'Address of any manager node already participating in the swarm.'<p/>
     * Node that it not docker JSON API address!<p/>
     */
    @JsonProperty("RemoteAddrs")
    private final List<String> managers = new ArrayList<>();

    /**
     * Secret token for joining this swarm.
     */
    @JsonProperty("JoinToken")
    private String token;
}
