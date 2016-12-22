package com.codeabovelab.dm.cluman.cluster.docker.model.swarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class ExternalCaConfig {

    @JsonProperty("Protocol")
    private String protocol;

    @JsonProperty("URL")
    private String url;

    @JsonProperty("Options")
    private Map<String, String> options;
}
