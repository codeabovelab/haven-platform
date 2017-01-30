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

import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 */
class NettyResponseHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final SettableListenableFuture<ClientHttpResponse> responseFuture;
    private final ChunkedInputStream<ByteBufHolder> in = new ChunkedInputStream<>(ByteBufHolderAdapter.INSTANCE);

    NettyResponseHandler(SettableListenableFuture<ClientHttpResponse> responseFuture) {
        this.responseFuture = responseFuture;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, HttpObject response) throws Exception {
        if(response instanceof HttpResponse) {
            this.responseFuture.set(new NettyResponse(context, (HttpResponse) response, in));
        } else if(response instanceof HttpContent) {
            HttpContent cont = (HttpContent) response;
            in.add(cont);
            if(response instanceof LastHttpContent) {
                in.end();
            }
        } else {
            throw new RuntimeException("Unknown message: " + response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        this.responseFuture.setException(cause);
    }

}
