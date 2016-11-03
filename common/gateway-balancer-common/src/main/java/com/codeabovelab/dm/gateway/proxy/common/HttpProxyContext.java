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
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;

/**
 * Class which contains some data for proxied request
 */
public final class HttpProxyContext {

    public static final String REQUEST_UUID = "x-request-uuid";

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final URI target;
    private final HttpHost targetHost;
    private final String uid;

    public HttpProxyContext(HttpServletRequest request, HttpServletResponse response, URI target, String uid) {
        this.request = request;
        Assert.notNull(request, "request is null");
        this.response = response;
        Assert.notNull(response, "response is null");
        this.target = target;
        Assert.notNull(target, "target is null");
        this.targetHost = new HttpHost(target.getHost(), target.getPort(), target.getScheme());
        this.uid = uid;

    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public URI getTarget() {
        return target;
    }

    public HttpHost getTargetHost() {
        return this.targetHost;
    }

    public String getTargetPath() {
        return this.target.getPath();
    }

    public String getUid() {
        return uid;
    }
}
