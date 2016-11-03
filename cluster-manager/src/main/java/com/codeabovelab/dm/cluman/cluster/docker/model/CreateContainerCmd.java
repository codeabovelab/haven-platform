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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import lombok.Data;

import java.util.Map;

/**
 * CreateContainer Rest model
 */
@Data
public class CreateContainerCmd {

    private String name;

    @JsonProperty("Hostname")
    private String hostName;

    @JsonProperty("Domainname")
    private String domainName;

    @JsonProperty("User")
    private String user;

    @JsonProperty("AttachStdin")
    private Boolean attachStdin;

    @JsonProperty("AttachStdout")
    private Boolean attachStdout;

    @JsonProperty("AttachStderr")
    private Boolean attachStderr;

    @JsonProperty("PortSpecs")
    private String[] portSpecs;

    @JsonProperty("Tty")
    private Boolean tty;

    @JsonProperty("OpenStdin")
    private Boolean stdinOpen;

    @JsonProperty("StdinOnce")
    private Boolean stdInOnce;

    @JsonProperty("Env")
    private String[] env;

    @JsonProperty("Cmd")
    private String[] cmd;

    @JsonProperty("Entrypoint")
    private String[] entrypoint;

    @JsonProperty("Image")
    private String image;

    @JsonProperty("Volumes")
    private Volumes volumes = new Volumes();

    @JsonProperty("WorkingDir")
    private String workingDir;

    @JsonProperty("MacAddress")
    private String macAddress;

    @JsonProperty("NetworkDisabled")
    private Boolean networkDisabled;

    @JsonProperty("ExposedPorts")
    private ExposedPorts exposedPorts;

    @JsonProperty("HostConfig")
    private HostConfig hostConfig;

    @JsonProperty("Labels")
    private Map<String, String> labels;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("hostName", hostName)
                .add("domainName", domainName)
                .add("user", user)
                .add("attachStdin", attachStdin)
                .add("attachStdout", attachStdout)
                .add("attachStderr", attachStderr)
                .add("portSpecs", portSpecs)
                .add("tty", tty)
                .add("stdinOpen", stdinOpen)
                .add("stdInOnce", stdInOnce)
                .add("env", env)
                .add("cmd", cmd)
                .add("entrypoint", entrypoint)
                .add("image", image)
                .add("volumes", volumes)
                .add("workingDir", workingDir)
                .add("macAddress", macAddress)
                .add("networkDisabled", networkDisabled)
                .add("exposedPorts", exposedPorts)
                .add("hostConfig", hostConfig)
                .add("labels", labels)
                .omitNullValues()
                .toString();
    }
}
