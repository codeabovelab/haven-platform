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
import com.google.common.base.Splitter;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
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
        if (checkCredentials()) {
            String tokenReq = ctx.getAuthenticate();
            String[] split = StringUtils.split(tokenReq, " ");
            Assert.isTrue(split.length == 2, "invalid token request " + tokenReq);
            String type = split[0];
            switch (type) {
                case "Bearer":
                    Map<String, String> map = Splitter.on(",").withKeyValueSeparator("=").split(split[1]);
                    AuthInfo authInfo = AuthInfo.builder()
                            .realm(map.get("realm"))
                            .service(map.get("service"))
                            .scope(map.get("scope")).build();
                    String token = getToken(authInfo);
                    ctx.getRequestHeaders().add(AUTHORIZATION, "Bearer " + token);
                    break;
                case "Basic":
                    ctx.getRequestHeaders().add(AUTHORIZATION, createBasicHeader(provider.getRegistryCredentials()));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid token string " + tokenReq);
            }
        }

    }

    @SuppressWarnings("unchecked")
    private String getToken(AuthInfo authInfo) {
        RegistryCredentials registryCredentials = provider.getRegistryCredentials();
        // realm="https://auth.docker.io/token",service="registry.docker.io"
        // get https://auth.docker.io/token?service=registry.docker.io
        try {
            URI path = getPath(authInfo);
            HttpEntity<String> request = new HttpEntity<>(
                    createHeaders(registryCredentials));
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

    private boolean checkCredentials() {
        RegistryCredentials registryCredentials = provider.getRegistryCredentials();
        return registryCredentials != null && StringUtils.hasText(registryCredentials.getUsername()) &&
                StringUtils.hasText(registryCredentials.getPassword());
    }

    private HttpHeaders createHeaders(RegistryCredentials registryCredentials) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (StringUtils.isEmpty(registryCredentials.getUsername())) {
            return httpHeaders;
        }
        httpHeaders.set(AUTHORIZATION, createBasicHeader(registryCredentials));
        return httpHeaders;
    }

    private String createBasicHeader(RegistryCredentials registryCredentials) {
        String auth = registryCredentials.getUsername() + ":" + registryCredentials.getPassword();
        return "Basic " + new String(Base64.encodeBase64(auth.getBytes()));
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

    @Value
    @Builder
    private static class AuthInfo {
        private final String realm;
        private final String service;
        private final String scope;
    }
}
