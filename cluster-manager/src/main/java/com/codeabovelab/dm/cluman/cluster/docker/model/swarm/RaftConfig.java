package com.codeabovelab.dm.cluman.cluster.docker.model.swarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RaftConfig {

    @JsonProperty("SnapshotInterval")
    private Integer snapshotInterval;

    @JsonProperty("KeepOldSnapshots")
    private Integer keepOldSnapshots;

    @JsonProperty("LogEntriesForSlowFollowers")
    private Integer logEntriesForSlowFollowers;

    @JsonProperty("ElectionTick")
    private Integer electionTick;

    @JsonProperty("HeartbeatTick")
    private Integer heartbeatTick;
}