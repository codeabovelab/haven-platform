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

package com.codeabovelab.dm.agent.dp;

import com.codeabovelab.dm.common.utils.Closeables;
import com.codeabovelab.dm.common.utils.Uuids;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.net.URI;
import java.nio.ByteBuffer;

/**
 */
@Slf4j
@Component
public class WsProxy extends Endpoint {

    @Autowired
    private Backend backend;
    private final String id = Uuids.longUid();

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        String id = session.getId();
        log.debug("{}: open ws proxy ", id);
        try {
            ChannelFuture cf = backend.connect().sync();
            Channel channel = cf.channel();
            WebSocketVersion version = Utils.getWsVersion(session.getProtocolVersion());
            WebSocketClientHandshaker wshs = WebSocketClientHandshakerFactory.newHandshaker(new URI(session.getQueryString()),
              version, null, true, new DefaultHttpHeaders());
            channel.pipeline().addLast(new WebSocketClientProtocolHandler(wshs), new WsHandler(session));
            log.debug("{}: wait messages", id);
            session.addMessageHandler(new WsHandler(session));
        } catch (Exception e) {
            log.error("{}: can not establish ws connect with backed", id, e);
        }

    }

    static class WsHandler extends ChannelInboundHandlerAdapter implements MessageHandler.Whole<ByteBuffer> {

        private final String id;
        private final Session session;
        private Channel channel;

        public WsHandler(Session session) {
            this.id = session.getId();
            this.session = session;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            this.channel = ctx.channel();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("{}: error in ws handler, close.", id, cause);
            internalClose();
        }

        @Override
        public void onMessage(ByteBuffer message) {
            channel.writeAndFlush(message);
        }

        private void internalClose() {
            Closeables.close(channel::close);
            Closeables.close(session);
        }
    }

}
