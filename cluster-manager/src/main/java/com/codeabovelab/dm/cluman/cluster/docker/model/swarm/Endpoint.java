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

import com.codeabovelab.dm.common.json.JtEnumLower;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * https://github.com/docker/docker/blob/master/api/types/swarm/network.go
 */
@Data
public class Endpoint {
    @JsonProperty("Spec")
    private final EndpointSpec spec;

    @JsonProperty("Ports")
    private final List<PortConfig> ports;

    @JsonProperty("VirtualIPs")
    private final List<EndpointVirtualIP> virtualIPs;

    @Data
    public static class EndpointSpec {
        /**
         * The mode of resolution to use for internal load balancing between tasks (vip or dnsrr).
         * Defaults to vip if not provided.
         */
        @JsonProperty("Mode")
        private final ResolutionMode mode;

        /**
         * List of exposed ports that this service is accessible on from the outside.
         * Ports can only be provided if vip resolution mode is used.
         */
        @JsonProperty("Ports")
        private final List<PortConfig> ports;
    }

    /**
     * The mode of resolution to use for internal load balancing between tasks (vip or dnsrr).
     */
    @JtEnumLower
    public enum ResolutionMode {
        VIP,
        DNSRR
    }

    /**
     * PortConfig represents the config of a port.
     */
    @Data
    public static class PortConfig {

        @JsonProperty("Name")
        private final String name;

        @JsonProperty("Protocol")
        private final Protocol protocol;

        /**
         * TargetPort is the port inside the container
         */
        @JsonProperty("TargetPort")
        private final int targetPort;

        /**
         * PublishedPort is the port on the swarm hosts
         */
        @JsonProperty("PublishedPort")
        private final int publishedPort;

        /**
         * PublishMode is the mode in which port is published
         */
        @JsonProperty("PublishMode")
        private final PortConfigPublishMode PublishMode;
    }

    @JtEnumLower
    public enum PortConfigPublishMode {
        /**
         * used for ports published
         * for ingress load balancing using routing mesh.
         */
        INGRESS,
        /**
         * used for ports published
         * for direct host level access on the host where the task is running.
         */
        HOST
    }

    @JtEnumLower
    public enum Protocol {
        TCP, UDP
    }

    /**
     * EndpointVirtualIP represents the virtual ip of a port.
     */
    @Data
    public static class EndpointVirtualIP {
        @JsonProperty("NetworkID")
        private final String networkId;
        @JsonProperty("Addr")
        private final String address;

    }
}
