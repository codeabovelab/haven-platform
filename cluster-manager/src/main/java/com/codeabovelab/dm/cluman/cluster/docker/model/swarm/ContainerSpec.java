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

import com.codeabovelab.dm.cluman.cluster.docker.model.HealthConfig;
import com.codeabovelab.dm.cluman.cluster.docker.model.Mount;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Container settings for containers started as part of task. <p/>
 * https://github.com/docker/docker/blob/master/api/types/swarm/container.go
 */
@Data
@AllArgsConstructor
@Builder(builderClassName = "Builder")
public class ContainerSpec {

    @JsonProperty("Image")
    private final String image;

    @JsonProperty("Labels")
    private final Map<String, String> labels;

    /**
     * The command to be run in the image.
     */
    @JsonProperty("Command")
    private final List<String> command;

    /**
     * Arguments to the command.
     */
    @JsonProperty("Args")
    private final List<String> args;

    @JsonProperty("Hostname")
    private final String hostname;

    /**
     * A list of environment variables in the form of ["VAR=value"[,"VAR2=value2"]].
     */
    @JsonProperty("Env")
    private final List<String> env;

    /**
     * A string specifying the working directory for commands to run in.
     */
    @JsonProperty("Dir")
    private final String dir;

    /**
     * A string value specifying the user inside the container.
     */
    @JsonProperty("User")
    private final String user;

    @JsonProperty("Groups")
    private final List<String> groups;

    @JsonProperty("TTY")
    private final boolean tty;

    @JsonProperty("OpenStdin")
    private final boolean stdin;

    @JsonProperty("Mounts")
    private final List<Mount> mounts;

    /**
     * Amount of time to wait for the container to terminate before forcefully killing it.
     */
    @JsonProperty("StopGracePeriod")
    private final long stopGracefullyPeriod;

    @JsonProperty("Healthcheck")
    private final HealthConfig healthcheck;

    /**
    * The format of extra hosts on swarmkit is specified in:
    * http://man7.org/linux/man-pages/man5/hosts.5.html
    *    IP_address canonical_hostname [aliases...]
    */
    @JsonProperty("Hosts")
    private final List<String> hosts;

    @JsonProperty("DNSConfig")
    private final DnsConfig dnsConfig;

//TODO https://github.com/docker/docker/blob/master/api/types/swarm/secret.go
//    Secrets   []*SecretReference

    @Data
    public static class DnsConfig {
        /**
         * specifies the IP addresses of the name servers
         */
        @JsonProperty("Nameservers")
        private final List<String> servers;

        /**
         * Search specifies the search list for host-name lookup
         */
        @JsonProperty("Search")
        private final List<String> search;

        /**
         * Options allows certain internal resolver variables to be modified
         */
        @JsonProperty("Options")
        private final List<String> options;
    }
}
