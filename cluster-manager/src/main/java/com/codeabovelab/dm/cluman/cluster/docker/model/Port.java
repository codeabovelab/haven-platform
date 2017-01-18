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


import com.codeabovelab.dm.cluman.model.ProtocolType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Docker container port
 */
public class Port {
    private static final String TYPE = "Type";
    private static final String PUBLIC_PORT = "PublicPort";
    private static final String PRIVATE_PORT = "PrivatePort";
    private static final String IP = "IP";

    private final String ip;
    private final int privatePort;
    private final int publicPort;
    private final ProtocolType type;

    public Port(@JsonProperty(IP) String ip,
                @JsonProperty(PRIVATE_PORT) int privatePort,
                @JsonProperty(PUBLIC_PORT) int publicPort,
                @JsonProperty(TYPE) ProtocolType type) {
        this.ip = ip;
        this.privatePort = privatePort;
        this.publicPort = publicPort;
        this.type = type;
    }

    @JsonProperty(IP)
    public String getIp() {
        return ip;
    }

    @JsonProperty(PRIVATE_PORT)
    public int getPrivatePort() {
        return privatePort;
    }

    @JsonProperty(PUBLIC_PORT)
    public int getPublicPort() {
        return publicPort;
    }

    @JsonProperty(TYPE)
    public ProtocolType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Port{" +
          "ip='" + ip + '\'' +
          ", privatePort=" + privatePort +
          ", publicPort=" + publicPort +
          ", type=" + type +
          '}';
    }
}
