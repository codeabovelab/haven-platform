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

import java.time.LocalDateTime;

/**
 * Docker task representation. <p/>
 * See https://github.com/docker/docker/blob/master/api/types/swarm/task.go
 */
@Data
public class Task {

    @JsonProperty("ID")
    private final String id;

    @JsonProperty("Version")
    private final SwarmVersion version;

    @JsonProperty("CreatedAt")
    private final LocalDateTime created;

    @JsonProperty("UpdatedAt")
    private final LocalDateTime updated;

    @Data
    public static class Spec {
        @JsonProperty("ContainerSpec")
        private final ContainerSpec container;
        @JsonProperty("Resources")
        private final ResourceRequirements resources;
        @JsonProperty("RestartPolicy")
        private final RestartPolicy restartPolicy;
    }

    /**
     *  Resource requirements which apply to each individual container created as part of the service.
     */
    @Data
    public static class ResourceRequirements {
        @JsonProperty("Limits")
        private final TaskResources limits;
        @JsonProperty("Reservations")
        private final TaskResources reservations;

    }

    /**
     * Specification for the restart policy which applies to containers created as part of this service.
     */
    @Data
    public static class RestartPolicy {
        @JsonProperty("Condition")
        private final RestartPolicyCondition condition;
        /**
         * Maximum attempts to restart a given container before giving up (default value is 0, which is ignored).
         */
        @JsonProperty("MaxAttempts")
        private final long max;
        /**
         * Delay between restart attempts.
         */
        @JsonProperty("Delay")
        private final long delay;
        /**
         * Windows is the time window used to evaluate the restart policy (default value is 0, which is unbounded).
         */
        @JsonProperty("Window")
        private final long window;
    }

    @JtEnumLower
    public enum RestartPolicyCondition {
        NONE,
        ON_FAILURE,
        ANY
    }

    @JtEnumLower
    public enum State {
        NEW,
        ALLOCATED,
        PENDING,
        ASSIGNED,
        ACCEPTED,
        PREPARING,
        READY,
        STARTING,
        RUNNING,
        COMPLETE,
        SHUTDOWN,
        FAILED,
        REJECTED
    }
}
