package com.codeabovelab.dm.cluman.cluster.docker.model.swarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class Driver {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Options")
    private Map<String, String> options;
}
