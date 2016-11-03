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

/**
 * Representation of a Docker event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DockerEvent {

    /**
     * Status of docker image or container. List of statuses is available in <a
     * href="https://docs.docker.com/reference/api/docker_remote_api_v1.16/#monitor-dockers-events">Docker API v.1.16</a>
     */
    private String status;

    /**
     * ID of docker container.
     *
     */
    private String id;

    /**
     * Source image of the container.
     *
     */
    private String from;

    private long time;

    /**
     * Returns the node when working against docker swarm
     */
    private Node node;

    @JsonProperty("Type")
    private EventType type;

    @JsonProperty("Action")
    private String action;

    @JsonProperty("Actor")
    private Actor actor;

}