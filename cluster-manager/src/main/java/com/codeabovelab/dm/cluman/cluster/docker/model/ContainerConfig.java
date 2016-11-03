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

import com.codeabovelab.dm.common.utils.Sugar;
import com.fasterxml.jackson.annotation.*;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Configuration of image container.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
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

    @JsonCreator
    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public ContainerConfig(@JsonProperty("AttachStderr") Boolean attachStderr,
                           @JsonProperty("AttachStdin") Boolean attachStdin,
                           @JsonProperty("AttachStdout") Boolean attachStdout,
                           @JsonProperty("Cmd") List<String> cmd,
                           @JsonProperty("Domainname") String domainName,
                           @JsonProperty("Entrypoint") List<String> entrypoint,
                           @JsonProperty("Env") List<String> env,
                           @JsonProperty("ExposedPorts") ExposedPorts exposedPorts,
                           @JsonProperty("Hostname") String hostName,
                           @JsonProperty("Image") String image,
                           @JsonProperty("Labels") Map<String, String> labels,
                           @JsonProperty("MacAddress") String macAddress,
                           @JsonProperty("NetworkDisabled") Boolean networkDisabled,
                           @JsonProperty("OnBuild") List<String> onBuild,
                           @JsonProperty("OpenStdin") Boolean stdinOpen,
                           @JsonProperty("PortSpecs") List<String> portSpecs,
                           @JsonProperty("StdinOnce") Boolean stdInOnce,
                           @JsonProperty("Tty") Boolean tty,
                           @JsonProperty("User") String user,
                           @JsonProperty("Volumes") Map<String, ?> volumes,
                           @JsonProperty("WorkingDir") String workingDir) {
        this.attachStderr = attachStderr;
        this.attachStdin = attachStdin;
        this.attachStdout = attachStdout;
        this.cmd = Sugar.immutableList(cmd);
        this.domainName = domainName;
        this.entrypoint = Sugar.immutableList(entrypoint);
        this.env = Sugar.immutableList(env);
        this.exposedPorts = exposedPorts;
        this.hostName = hostName;
        this.image = image;
        this.labels = Sugar.immutableMap(labels);
        this.macAddress = macAddress;
        this.networkDisabled = networkDisabled;
        this.onBuild = Sugar.immutableList(onBuild);
        this.stdinOpen = stdinOpen;
        this.portSpecs = Sugar.immutableList(portSpecs);
        this.stdInOnce = stdInOnce;
        this.tty = tty;
        this.user = user;
        this.volumes = Sugar.immutableMap(volumes);
        this.workingDir = workingDir;
    }

    @JsonIgnore
    public List<ExposedPort> getExposedPorts() {
        return exposedPorts != null ? exposedPorts.getExposedPorts() : null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("attachStderr", attachStderr)
                .add("attachStdin", attachStdin)
                .add("attachStdout", attachStdout)
                .add("cmd", cmd)
                .add("domainName", domainName)
                .add("entrypoint", entrypoint)
                .add("env", env)
                .add("exposedPorts", exposedPorts)
                .add("hostName", hostName)
                .add("image", image)
                .add("labels", labels)
                .add("macAddress", macAddress)
                .add("networkDisabled", networkDisabled)
                .add("onBuild", onBuild)
                .add("stdinOpen", stdinOpen)
                .add("portSpecs", portSpecs)
                .add("stdInOnce", stdInOnce)
                .add("tty", tty)
                .add("user", user)
                .add("volumes", volumes)
                .add("workingDir", workingDir)
                .omitNullValues()
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContainerConfig that = (ContainerConfig) o;
        return Objects.equal(attachStderr, that.attachStderr) &&
                Objects.equal(attachStdin, that.attachStdin) &&
                Objects.equal(attachStdout, that.attachStdout) &&
                Objects.equal(cmd, that.cmd) &&
                Objects.equal(domainName, that.domainName) &&
                Objects.equal(entrypoint, that.entrypoint) &&
                Objects.equal(env, that.env) &&
                Objects.equal(exposedPorts, that.exposedPorts) &&
                Objects.equal(hostName, that.hostName) &&
                Objects.equal(image, that.image) &&
                Objects.equal(labels, that.labels) &&
                Objects.equal(macAddress, that.macAddress) &&
                Objects.equal(networkDisabled, that.networkDisabled) &&
                Objects.equal(onBuild, that.onBuild) &&
                Objects.equal(stdinOpen, that.stdinOpen) &&
                Objects.equal(portSpecs, that.portSpecs) &&
                Objects.equal(stdInOnce, that.stdInOnce) &&
                Objects.equal(tty, that.tty) &&
                Objects.equal(user, that.user) &&
                Objects.equal(volumes, that.volumes) &&
                Objects.equal(workingDir, that.workingDir);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(attachStderr, attachStdin, attachStdout, cmd, domainName, entrypoint, env, exposedPorts,
                hostName, image, labels, macAddress, networkDisabled, onBuild, stdinOpen, portSpecs, stdInOnce, tty, user, volumes, workingDir);
    }
}