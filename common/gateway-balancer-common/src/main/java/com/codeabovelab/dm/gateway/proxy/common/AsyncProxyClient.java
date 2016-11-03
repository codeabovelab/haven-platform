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

package com.codeabovelab.dm.gateway.proxy.common;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import java.io.IOException;

public class AsyncProxyClient implements ProxyClient {

    private final CloseableHttpAsyncClient proxyClient;

    public AsyncProxyClient(CloseableHttpAsyncClient proxyClient) {
        this.proxyClient = proxyClient;
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request) throws Exception {
        return proxyClient.execute(target, request, null).get();
    }

    @Override
    public void start() {
        proxyClient.start();
    }

    @Override
    public void close() throws IOException {
        proxyClient.close();
    }
}
