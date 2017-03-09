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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;

class RegistryAuthInterceptor implements ClientHttpRequestInterceptor {

    private final RegistryAuthAdapter adapter;

    RegistryAuthInterceptor(RegistryAuthAdapter adapter) {
        this.adapter = adapter;
        Assert.notNull(adapter, "adapter can't be null");
    }

    @Override
    public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
                                        final ClientHttpRequestExecution execution) throws IOException {
        final HttpHeaders headers = request.getHeaders();
        ClientHttpResponse execute = execution.execute(request, body);

        if (execute.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            List<String> list = execute.getHeaders().get("Www-Authenticate");
            if (!CollectionUtils.isEmpty(list)) {
                String tokenString = list.get(0);
                RegistryAuthAdapter.AuthContext ctx = new RegistryAuthAdapter.AuthContext(headers,
                  HttpHeaders.readOnlyHttpHeaders(headers),
                  tokenString);
                adapter.handle(ctx);
                return execution.execute(request, body);
            }
        }
        return execute;
    }

}