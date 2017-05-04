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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @JsonProperty("Name")
    private final String name;

    @JsonProperty("Labels")
    private final Map<String, String> labels;

    @JsonProperty("Spec")
    private final TaskSpec spec;

    @JsonProperty("ServiceID")
    private final String serviceId;

    @JsonProperty("Slot")
    private final int slot;

    @JsonProperty("NodeID")
    private final String nodeId;

    @JsonProperty("Status")
    private final TaskStatus status;

    @JsonProperty("DesiredState")
    private final TaskState desiredState;

    @JsonProperty("NetworksAttachments")
    private final List<SwarmNetwork.NetworkAttachment> networksAttachments;

    @Data
    @AllArgsConstructor
    @Builder(builderClassName = "Builder")
    public static class TaskSpec {

        @JsonProperty("ContainerSpec")
        private final ContainerSpec container;

        @JsonProperty("Resources")
        private final ResourceRequirements resources;

        @JsonProperty("RestartPolicy")
        private final RestartPolicy restartPolicy;

        @JsonProperty("Placement")
        private final Placement placement;

        @JsonProperty("Networks")
        private final List<SwarmNetwork.NetworkAttachmentConfig> networks;

        /**
         * LogDriver specifies the LogDriver to use for tasks created from this
         * spec. If not present, the one on cluster default on swarm.Spec will be
         * used, finally falling back to the engine default if not specified.
         */
        @JsonProperty("LogDriver")
        private final Driver logDriver;

        /**
         * ForceUpdate is a counter that triggers an update even if no relevant
         * parameters have been changed.
         */
        @JsonProperty("ForceUpdate")
        private final long forceUpdate;
    }

    /**
     * Resource requirements which apply to each individual container created as part of the service.
     */
    @Data
    @AllArgsConstructor
    @Builder(builderClassName = "Builder")
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
    @AllArgsConstructor
    @Builder(builderClassName = "Builder")
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
    public enum TaskState {
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

    /**
     * TaskStatus represents the status of a task.
     */
    @Data
    public static class TaskStatus {

        @JsonProperty("Timestamp")
        private final LocalDateTime timestamp;

        @JsonProperty("State")
        private final TaskState state;

        @JsonProperty("Message")
        private final String message;

        @JsonProperty("Err")
        private final String error;

        @JsonProperty("ContainerStatus")
        private final ContainerStatus containerStatus;

        @JsonProperty("PortStatus")
        private final PortStatus portStatus;
    }

    /**
     * ContainerStatus represents the status of a container.
     */
    @Data
    public static class ContainerStatus {

        @JsonProperty("ContainerID")
        private final String containerId;

        @JsonProperty("PID")
        private final int pid;

        @JsonProperty("ExitCode")
        private final int exitCode;
    }

    /**
     * PortStatus represents the port status of a task's host ports whose
     * service has published host ports
     */
    @Data
    @AllArgsConstructor
    @Builder(builderClassName = "Builder")
    public static class PortStatus {

        @JsonProperty("Ports")
        private final List<Endpoint.PortConfig> ports;
    }

    /**
     * Placement represents orchestration parameters.
     */
    @Data
    @AllArgsConstructor
    @Builder(builderClassName = "Builder")
    public static class Placement {

        @JsonProperty("Constraints")
        private final List<String> constraints;
    }
}
