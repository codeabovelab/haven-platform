package com.codeabovelab.dm.cluman.cluster.docker.model.swarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TaskDefaults {

    @JsonProperty("LogDriver")
    private Driver logDriver;
}
