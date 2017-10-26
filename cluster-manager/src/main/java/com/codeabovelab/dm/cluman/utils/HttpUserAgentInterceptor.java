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
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 */
public class HttpUserAgentInterceptor implements ClientHttpRequestInterceptor {

    private static final HttpUserAgentInterceptor DEFAULT = new HttpUserAgentInterceptor("Haven");

    private final String userAgent;

    public HttpUserAgentInterceptor(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().set("User-Agent", "Haven");
        return execution.execute(request, body);
    }

    /**
     * Interceptor which install default user-agent.
     * @return interceptor
     */
    public static HttpUserAgentInterceptor getDefault() {
        return DEFAULT;
    }
}
