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

package com.codeabovelab.dm.platform.http.async;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * We create our implementation based on {@link org.springframework.http.client.Netty4ClientHttpResponse }
 * due to need consume of endless stream with "TransferEncoding: chunked", which default implementation does not allow.
 */
class NettyResponse implements ClientHttpResponse {
    private final ChannelHandlerContext context;

    private final HttpResponse nettyResponse;

    private final InputStream body;

    private volatile HttpHeaders headers;


    NettyResponse(ChannelHandlerContext context, HttpResponse nettyResponse, InputStream body) {
        Assert.notNull(context, "ChannelHandlerContext must not be null");
        Assert.notNull(nettyResponse, "FullHttpResponse must not be null");
        this.context = context;
        this.nettyResponse = nettyResponse;
        this.body = body;
    }

    @Override
    public HttpStatus getStatusCode() throws IOException {
        return HttpStatus.valueOf(getRawStatusCode());
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getRawStatusCode() throws IOException {
        return this.nettyResponse.getStatus().code();
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getStatusText() throws IOException {
        return this.nettyResponse.getStatus().reasonPhrase();
    }

    @Override
    public HttpHeaders getHeaders() {
        if (this.headers == null) {
            HttpHeaders headers = new HttpHeaders();
            for (Map.Entry<String, String> entry : this.nettyResponse.headers()) {
                headers.add(entry.getKey(), entry.getValue());
            }
            this.headers = headers;
        }
        return this.headers;
    }

    @Override
    public InputStream getBody() throws IOException {
        return this.body;
    }

    @Override
    public void close() {
        this.context.close();
    }

}

