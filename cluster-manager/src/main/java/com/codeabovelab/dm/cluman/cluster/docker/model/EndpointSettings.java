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

package com.codeabovelab.dm.cluman.cluster.docker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * https://github.com/docker/docker/blob/b2e348f2a6ab2d5396acf4bb56aea7e49c3e2097/api/types/network/network.go#L38
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@lombok.Builder(builderClassName = "Builder")
@AllArgsConstructor
public class EndpointSettings {

    @JsonProperty("IPAMConfig")
    private final EndpointIPAMConfig ipamConfig;

    @JsonProperty("NetworkID")
    private final String networkID;

    @JsonProperty("EndpointID")
    private final String endpointId;

    @JsonProperty("Gateway")
    private final String gateway;

    @JsonProperty("IPAddress")
    private final String ipAddress;

    @JsonProperty("IPPrefixLen")
    private final Integer ipPrefixLen;

    @JsonProperty("IPv6Gateway")
    private final String ipV6Gateway;

    @JsonProperty("GlobalIPv6Address")
    private final String globalIPv6Address;

    @JsonProperty("GlobalIPv6PrefixLen")
    private final Integer globalIPv6PrefixLen;

    @JsonProperty("MacAddress")
    private final String macAddress;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @lombok.Builder(builderClassName = "Builder")
    @AllArgsConstructor
    public static class EndpointIPAMConfig {
        @JsonProperty("IPv4Address")
        private final String ipv4Address;

        @JsonProperty("IPv6Address")
        private final String ipv6Address;

        @JsonProperty("LinkLocalIPs")
        private final List<String> linkLocalIPs;
    }
}
