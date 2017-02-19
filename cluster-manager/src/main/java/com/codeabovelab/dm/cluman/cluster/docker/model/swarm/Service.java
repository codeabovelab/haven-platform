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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * https://github.com/docker/docker/blob/master/api/types/swarm/service.go
 */
@Data
public class Service {
    @JsonProperty("ID")
    private final String id;
    @JsonProperty("Version")
    private final SwarmVersion version;
    @JsonProperty("CreatedAt")
    private final LocalDateTime created;
    @JsonProperty("UpdatedAt")
    private final LocalDateTime updated;
    @JsonProperty("Spec")
    private final ServiceSpec spec;
    @JsonProperty("PreviousSpec")
    private final ServiceSpec previousSpec;
    @JsonProperty("Endpoint")
    private final Endpoint endpoint;
    @JsonProperty("UpdateStatus")
    private final UpdateStatus updateStatus;

    @Data
    public static class UpdateStatus {
        @JsonProperty("State")
        private final UpdateState state;
        @JsonProperty("StartedAt")
        private final LocalDateTime started;
        @JsonProperty("CompletedAt")
        private final LocalDateTime completed;
        @JsonProperty("Message")
        private final String message;
    }

    @JtEnumLower
    public enum UpdateState {
        UPDATING,
        PAUSED,
        COMPLETED
    }

    @Data
    @AllArgsConstructor
    @Builder(builderClassName = "Builder", toBuilder = true)
    public static class ServiceSpec {

        @JsonProperty("Name")
        private final String name;

        @JsonProperty("Labels")
        private final Map<String, String> labels;


        /**
         * TaskTemplate defines how the service should construct new tasks when
         * orchestrating this service.
         */
        @JsonProperty("TaskTemplate")
        private final Task.TaskSpec taskTemplate;

        @JsonProperty("Mode")
        private final ServiceMode mode;

        @JsonProperty("UpdateConfig")
        private final UpdateConfig updateConfig;

        /** Networks field in ServiceSpec is deprecated. The
         same field in TaskSpec should be used instead.
         This field will be removed in a future release.
         Networks     []NetworkAttachmentConfig
         */

        /**
         * Properties that can be configured to access and load balance a service.
         */
        @JsonProperty("EndpointSpec")
        private final Endpoint.EndpointSpec endpointSpec;
    }

    @Data
    @AllArgsConstructor
    @Builder(builderClassName = "Builder")
    public static class ServiceMode {

        @JsonProperty("Replicated")
        private final ReplicatedService replicated;

        @JsonProperty("Global")
        private final GlobalService global;
    }

    /**
     * ReplicatedService is a kind of ServiceMode.
     */
    @Data
    public static class ReplicatedService {

        @JsonProperty("Replicas")
        private final long replicas;
    }

    /**
     * GlobalService is a kind of ServiceMode.
     */
    @Data
    public static class GlobalService {

    }

    /**
     * UpdateConfig represents the update configuration.
     */
    @Data
    @AllArgsConstructor
    @Builder(builderClassName = "Builder")
    public static class UpdateConfig {
        /**
         * Maximum number of tasks to be updated in one iteration. <p/>
         * 0 means unlimited parallelism.
         */
        @JsonProperty("Parallelism")
        private final long parallelism;

        /**
         * Amount of time between updates.
         */
        @JsonProperty("Delay")
        private final long delay;

        /**
         * FailureAction is the action to take when an update failures.
         */
        @JsonProperty("FailureAction")
        private final FailureAction failureAction;

        /**
         * Monitor indicates how long to monitor a task for failure after it is
         * created. If the task fails by ending up in one of the states
         * REJECTED, COMPLETED, or FAILED, within Monitor from its creation,
         * this counts as a failure. If it fails after Monitor, it does not
         * count as a failure. If Monitor is unspecified, a default value will
         * be used.
         */
        @JsonProperty("Monitor")
        private final long monitor;

        /**
         * MaxFailureRatio is the fraction of tasks that may fail during
         * an update before the failure action is invoked. Any task created by
         * the current update which ends up in one of the states REJECTED,
         * COMPLETED or FAILED within Monitor from its creation counts as a
         * failure. The number of failures is divided by the number of tasks
         * being updated, and if this fraction is greater than
         * MaxFailureRatio, the failure action is invoked. <p/>
         * If the failure action is CONTINUE, there is no effect. <p/>
         * If the failure action is PAUSE, no more tasks will be updated until
         * another update is started.
         */
        @JsonProperty("MaxFailureRatio")
        private final float maxFailureRatio;
    }

    @JtEnumLower
    public enum FailureAction {
        PAUSE, CONTINUE
    }
}
