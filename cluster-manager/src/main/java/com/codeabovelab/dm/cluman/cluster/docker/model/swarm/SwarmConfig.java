package com.codeabovelab.dm.cluman.cluster.docker.model.swarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 "Spec": {
     "Orchestration": {},
     "Raft": {},
     "Dispatcher": {},
     "CAConfig": {}
    }
 Also can be used for update command
 */
@Data
public class SwarmConfig {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Labels")
    private Map<String, String> labels;

    @JsonProperty("Orchestration")
    private OrchestrationConfig orchestration;

    @JsonProperty("Raft")
    private RaftConfig raft;

    @JsonProperty("Dispatcher")
    private DispatcherConfig dispatcher;

    @JsonProperty("CAConfig")
    private CaConfig caConfig;

    @JsonProperty("TaskDefaults")
    private TaskDefaults taskDefaults;

}
