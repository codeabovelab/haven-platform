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

import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Configuration of image container.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class ContainerConfig {

    @JsonProperty("AttachStderr")
    private final Boolean attachStderr;

    @JsonProperty("AttachStdin")
    private final Boolean attachStdin;

    @JsonProperty("AttachStdout")
    private final Boolean attachStdout;

    @JsonProperty("Cmd")
    private final List<String> cmd;

    @JsonProperty("Domainname")
    private final String domainName;

    @JsonProperty("Entrypoint")
    private final List<String> entrypoint;

    @JsonProperty("Env")
    private final List<String> env;

    @JsonProperty("ExposedPorts")
    private final ExposedPorts exposedPorts;

    @JsonProperty("Hostname")
    private final String hostName;

    @JsonProperty("Image")
    private final String image;

    @JsonProperty("Labels")
    private final Map<String, String> labels;

    @JsonProperty("MacAddress")
    private final String macAddress;

    @JsonProperty("NetworkDisabled")
    private final Boolean networkDisabled;

    @JsonProperty("OnBuild")
    private final List<String> onBuild;

    @JsonProperty("OpenStdin")
    private final Boolean stdinOpen;

    @JsonProperty("PortSpecs")
    private final List<String> portSpecs;

    @JsonProperty("StdinOnce")
    private final Boolean stdInOnce;

    @JsonProperty("Tty")
    private final Boolean tty;

    @JsonProperty("User")
    private final String user;

    @JsonProperty("Volumes")
    private final Map<String, ?> volumes;

    @JsonProperty("WorkingDir")
    private final String workingDir;

    @JsonIgnore
    public List<ExposedPort> getExposedPorts() {
        return exposedPorts != null ? exposedPorts.getExposedPorts() : null;
    }

}