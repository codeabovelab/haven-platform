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

package com.codeabovelab.dm.cluman.utils;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Based on {@link org.springframework.http.client.support.BasicAuthorizationInterceptor } which is not support
 * async client.
 */
public class BasicAuthAsyncInterceptor implements AsyncClientHttpRequestInterceptor {

    private final String username;

    private final String password;

    public BasicAuthAsyncInterceptor(String username, String password) {
        Assert.hasLength(username, "Username must not be empty");
        this.username = username;
        this.password = (password != null ? password : "");
    }

    @Override
    public ListenableFuture<ClientHttpResponse> intercept(HttpRequest request, byte[] body, AsyncClientHttpRequestExecution execution) throws IOException {
        String token = Base64Utils.encodeToString((this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));
        request.getHeaders().add("Authorization", "Basic " + token);
        return execution.executeAsync(request, body);
    }
}
