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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import lombok.Data;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class NetworkSettings {

    @JsonProperty("Bridge")
    private String bridge;

    @JsonProperty("SandboxID")
    private String sandboxId;

    @JsonProperty("HairpinMode")
    private Boolean hairpinMode;

    @JsonProperty("LinkLocalIPv6Address")
    private String linkLocalIPv6Address;

    @JsonProperty("LinkLocalIPv6PrefixLen")
    private Integer linkLocalIPv6PrefixLen;

    @JsonProperty("Ports")
    private Ports ports;

    @JsonProperty("SandboxKey")
    private String sandboxKey;

    @JsonProperty("SecondaryIPAddresses")
    private Object secondaryIPAddresses;

    @JsonProperty("SecondaryIPv6Addresses")
    private Object secondaryIPv6Addresses;

    @JsonProperty("EndpointID")
    private String endpointID;

    @JsonProperty("Gateway")
    private String gateway;

    @JsonProperty("PortMapping")
    private Map<String, Map<String, String>> portMapping;

    @JsonProperty("GlobalIPv6Address")
    private String globalIPv6Address;

    @JsonProperty("GlobalIPv6PrefixLen")
    private Integer globalIPv6PrefixLen;

    @JsonProperty("IPAddress")
    private String ipAddress;

    @JsonProperty("IPPrefixLen")
    private Integer ipPrefixLen;

    @JsonProperty("IPv6Gateway")
    private String ipV6Gateway;

    @JsonProperty("MacAddress")
    private String macAddress;

    @JsonProperty("Networks")
    private Map<String, Network> networks;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Network {

        @JsonProperty("IPAMConfig")
        private String ipamConfig;

        /**
         * @since {@link RemoteApiVersion#VERSION_1_22}
         */
        @JsonProperty("NetworkID")
        private String networkID;

        @JsonProperty("EndpointID")
        private String endpointId;

        @JsonProperty("Gateway")
        private String gateway;

        @JsonProperty("IPAddress")
        private String ipAddress;

        @JsonProperty("IPPrefixLen")
        private Integer ipPrefixLen;

        @JsonProperty("IPv6Gateway")
        private String ipV6Gateway;

        @JsonProperty("GlobalIPv6Address")
        private String globalIPv6Address;

        @JsonProperty("GlobalIPv6PrefixLen")
        private Integer globalIPv6PrefixLen;

        @JsonProperty("MacAddress")
        private String macAddress;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("ipamConfig", ipamConfig)
                    .add("networkID", networkID)
                    .add("endpointId", endpointId)
                    .add("gateway", gateway)
                    .add("ipAddress", ipAddress)
                    .add("ipPrefixLen", ipPrefixLen)
                    .add("ipV6Gateway", ipV6Gateway)
                    .add("globalIPv6Address", globalIPv6Address)
                    .add("globalIPv6PrefixLen", globalIPv6PrefixLen)
                    .add("macAddress", macAddress)
                    .omitNullValues().toString();
        }
    }
}
