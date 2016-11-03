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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Network {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Scope")
    private String scope;

    @JsonProperty("Driver")
    private String driver;

    @JsonProperty("IPAM")
    private Ipam ipam;

    @JsonProperty("Containers")
    private Map<String, ContainerNetworkConfig> containers;

    @JsonProperty("Options")
    private Map<String, String> options;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContainerNetworkConfig {

        @JsonProperty("EndpointID")
        private String endpointId;

        @JsonProperty("MacAddress")
        private String macAddress;

        @JsonProperty("IPv4Address")
        private String ipv4Address;

        @JsonProperty("IPv6Address")
        private String ipv6Address;

    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ipam {

        @JsonProperty("Driver")
        private String driver;

        @JsonProperty("Config")
        List<Config> config = new ArrayList<>();

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Config {

            @JsonProperty("Subnet")
            private String subnet;

            @JsonProperty("IPRange")
            private String ipRange;

            @JsonProperty("Gateway")
            private String gateway;

        }
    }

    @Override
    public String toString() {
        return  "{id='" + id + '\'' +
                ", driver='" + driver + '\'' +
                ", name='" + name + '\'' + '}';
    }
}