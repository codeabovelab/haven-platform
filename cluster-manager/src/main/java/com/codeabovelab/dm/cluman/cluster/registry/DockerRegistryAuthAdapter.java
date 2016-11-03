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

package com.codeabovelab.dm.cluman.cluster.registry;

import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryAuthAdapter;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryCredentials;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryCredentialsProvider;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;

import static org.springframework.web.util.UriComponentsBuilder.newInstance;

@Slf4j
class DockerRegistryAuthAdapter implements RegistryAuthAdapter {

    private final RestTemplate restTemplate = new RestTemplate();
    private final RegistryCredentialsProvider provider;

    DockerRegistryAuthAdapter(RegistryCredentialsProvider provider) {
        this.provider = provider;
    }

    @Override
    public void handle(AuthContext ctx) {
        String tokenString = ctx.getAuthenticate();
        AuthInfo authInfo = parse(tokenString);
        String token = getToken(authInfo);
        ctx.getRequestHeaders().add("Authorization", "Bearer " + token);
    }


    private AuthInfo parse(String token) {
        String bearer = token.replace("Bearer", "");
        String[] split = bearer.split(",");
        AuthInfo authInfo = new AuthInfo();
        for (String s : split) {
            String[] keyValue = s.trim().split("=");
            switch (keyValue[0]) {
                case "realm":
                    authInfo.setRealm(keyValue[1].replace("\"", ""));
                    break;
                case "service":
                    authInfo.setService(keyValue[1].replace("\"", ""));
                    break;
                case "scope":
                    authInfo.setScope(keyValue[1].replace("\"", ""));
            }
        }
        return authInfo;
    }

    @SuppressWarnings("unchecked")
    private String getToken(AuthInfo authInfo) {
        RegistryCredentials registryCredentials = provider.getRegistryCredentials();
        // realm="https://auth.docker.io/token",service="registry.docker.io"
        // get https://auth.docker.io/token?service=registry.docker.io
        try {
            URI path = getPath(authInfo);
            HttpEntity<String> request = null;
            if (registryCredentials != null && StringUtils.hasText(registryCredentials.getUsername()) &&
                    StringUtils.hasText(registryCredentials.getPassword())) {
                request = new HttpEntity<>(
                        createHeaders(registryCredentials.getUsername(), registryCredentials.getPassword()));
            }
            Map<String, String> token = restTemplate.exchange(path, HttpMethod.GET, request, Map.class).getBody();

            if (!token.isEmpty()) {
                return token.get("token");
            }
        } catch (HttpClientErrorException e) {
            log.error("Can't do request " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Can't do request", e);
        }
        return null;
    }

    private HttpHeaders createHeaders(String username, String password) {
        if (StringUtils.isEmpty(username)) {
            return new HttpHeaders();
        }
        return new HttpHeaders() {
            {
                String auth = username + ":" + password;
                byte[] encodedAuth = Base64.encodeBase64(
                        auth.getBytes(Charset.forName("US-ASCII")));
                String authHeader = "Basic " + new String(encodedAuth);
                set("Authorization", authHeader);
            }
        };
    }

    private URI getPath(AuthInfo authInfo) throws URISyntaxException {
        UriComponentsBuilder builder = newInstance()
                .uri(new URI(authInfo.getRealm()))
                .queryParam("service", authInfo.getService());
        String scope = authInfo.getScope();
        if (StringUtils.hasText(scope)) {
            builder.queryParam("scope", scope);
        }
        return builder.build().toUri();
    }

    @Data
    private static class AuthInfo {
        private String realm;
        private String service;
        private String scope;
    }
}
