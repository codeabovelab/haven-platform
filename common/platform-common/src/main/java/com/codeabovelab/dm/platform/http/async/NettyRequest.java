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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * We create our implementation based on {@link org.springframework.http.client.Netty4ClientHttpRequest }
 * due to need consume of endless stream with "TransferEncoding: chunked", which default implementation does not allow.
 */
class NettyRequest implements ClientHttpRequest, AsyncClientHttpRequest {
    private final HttpHeaders headers = new HttpHeaders();

    private final Bootstrap bootstrap;

    private final URI uri;

    private final HttpMethod method;

    private final ByteBufOutputStream body;

    private boolean executed = false;

    NettyRequest(Bootstrap bootstrap, URI uri, HttpMethod method) {
        this.bootstrap = bootstrap;
        this.uri = uri;
        this.method = method;
        this.body = new ByteBufOutputStream(Unpooled.buffer(1024));
    }

    @Override
    public final HttpHeaders getHeaders() {
        return (this.executed ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
    }

    @Override
    public final OutputStream getBody() throws IOException {
        assertNotExecuted();
        return getBodyInternal(this.headers);
    }

    @Override
    public ListenableFuture<ClientHttpResponse> executeAsync() throws IOException {
        assertNotExecuted();
        ListenableFuture<ClientHttpResponse> result = executeInternal(this.headers);
        this.executed = true;
        return result;
    }

    /**
     * Asserts that this request has not been {@linkplain #executeAsync() executed} yet.
     *
     * @throws IllegalStateException if this request has been executed
     */
    protected void assertNotExecuted() {
        Assert.state(!this.executed, "ClientHttpRequest already executed");
    }

    @Override
    public HttpMethod getMethod() {
        return this.method;
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    protected OutputStream getBodyInternal(HttpHeaders headers) {
        return this.body;
    }

    protected ListenableFuture<ClientHttpResponse> executeInternal(final HttpHeaders headers) {
        final SettableListenableFuture<ClientHttpResponse> responseFuture = new SettableListenableFuture<>();

        ChannelFutureListener connectionListener = future -> {
            if (future.isSuccess()) {
                Channel channel = future.channel();
                channel.pipeline().addLast(new NettyResponseHandler(responseFuture));
                FullHttpRequest nettyRequest = createFullHttpRequest(headers);
                channel.writeAndFlush(nettyRequest);
            }
            else {
                responseFuture.setException(future.cause());
            }
        };

        this.bootstrap.connect(this.uri.getHost(), getPort(this.uri)).addListener(connectionListener);

        return responseFuture;
    }

    @Override
    public ClientHttpResponse execute() throws IOException {
        try {
            return executeAsync().get();
        } catch (InterruptedException ex) {
            throw new IOException(ex.getMessage(), ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            else {
                throw new IOException(ex.getMessage(), ex);
            }
        }
    }

    private static int getPort(URI uri) {
        int port = uri.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                port = 80;
            } else if ("https".equalsIgnoreCase(uri.getScheme())) {
                port = 443;
            }
        }
        return port;
    }

    private FullHttpRequest createFullHttpRequest(HttpHeaders headers) {
        io.netty.handler.codec.http.HttpMethod nettyMethod =
          io.netty.handler.codec.http.HttpMethod.valueOf(this.method.name());

        FullHttpRequest nettyRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
          nettyMethod, this.uri.toString(), this.body.buffer());

        io.netty.handler.codec.http.HttpHeaders nettyHeaders = nettyRequest.headers();
        nettyHeaders.set(HttpHeaders.HOST, this.uri.getHost());
        nettyHeaders.set(HttpHeaders.CONNECTION, "close");

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            nettyHeaders.add(entry.getKey(), entry.getValue());
        }

        return nettyRequest;
    }

}