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

package com.codeabovelab.dm.cluman.cluster.docker.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Data which include as 'X-Registry-Auth – base64-encoded'. <p/>
 * It containing either login information, or a token <p/>
 * Credential based login: <p/>
 * <code>{ "username": "jdoe", "password": "secret", "email": "jdoe@acme.com" }</code> <p/>
 * Token based login: <p/>
 * <code>{ "identitytoken": "9cbaf023786cd7..." }</code> <p/>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder(builderClassName = "Builder")
public class AuthConfig {
    private final String username;
    private final String password;
    private final String email;
    @JsonProperty("identitytoken")
    private final String token;

    public static void install(HttpHeaders headers, AuthConfig ac) {
        if(ac == null) {
            return;
        }
        ObjectMapper om = new ObjectMapper();
        String str;
        try {
            str = om.writeValueAsString(ac);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can not serialize " + ac, e);
        }
        String coded = Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
        headers.set("X-Registry-Auth", coded);
    }
}
