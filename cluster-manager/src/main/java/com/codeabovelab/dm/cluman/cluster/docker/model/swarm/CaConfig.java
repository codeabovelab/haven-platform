package com.codeabovelab.dm.cluman.cluster.docker.model.swarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CaConfig {

    @JsonProperty("NodeCertExpiry")
    private Long nodeCertExpiry;

    @JsonProperty("ExternalCAs")
    private List<ExternalCaConfig> externalCas;

}
