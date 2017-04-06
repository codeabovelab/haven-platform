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
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * Rest controller which serves proxy requests
 */
@Slf4j
public class ProxyServlet extends GenericServlet {


    private final Bootstrap bootstrap;
    private final EpollEventLoopGroup group;

    @Autowired
    public ProxyServlet() {
        this.bootstrap = new Bootstrap();
        this.group = new EpollEventLoopGroup();
        bootstrap.group(group)
          .channel(EpollDomainSocketChannel.class)
          .handler(new ChannelInitializer<DomainSocketChannel>() {
                @Override
                protected void initChannel(DomainSocketChannel channel) throws Exception {
                    channel.pipeline().addLast(new HttpClientCodec());
                }
            }
          );
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;
        try {
            System.out.println("BEFORE CONNECT");
            ChannelFuture cf = bootstrap.connect(new DomainSocketAddress("/var/run/docker.sock")).sync();
            Channel channel = cf.channel();
            channel.pipeline().addLast(new Handler(response));
            DefaultFullHttpRequest backendReq = buildRequest(request);
            System.out.println("BEFORE SEND " + backendReq);
            channel.writeAndFlush(backendReq).sync();
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    private DefaultFullHttpRequest buildRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        DefaultFullHttpRequest br = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri);
        Enumeration<String> headers = request.getHeaderNames();
        HttpHeaders bh = br.headers();
        while(headers.hasMoreElements()) {
            String header = headers.nextElement();
            Iterable<String> iter = () -> Iterators.forEnumeration(request.getHeaders(header));
            bh.add(header, iter);
        }
        return br;
    }

    @Override
    public void destroy() {
        group.shutdownGracefully();
    }

    // TODO add identifier for each request (for logging purposes)
    // implement web socket & chunked streaming
    private static class Handler extends ChannelInboundHandlerAdapter {
        private final HttpServletResponse frontResp;
        private volatile ServletOutputStream stream;
        private volatile boolean hasError;

        Handler(HttpServletResponse frontResp) {
            this.frontResp = frontResp;
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            System.out.println("READ " + msg);
            if(msg instanceof HttpResponse) {
                HttpResponse backendResp = (HttpResponse) msg;
                HttpResponseStatus status = backendResp.status();
                frontResp.setStatus(status.code());
            } else if(msg instanceof HttpContent) {
                HttpContent backendResp = (HttpContent) msg;
                if(backendResp == LastHttpContent.EMPTY_LAST_CONTENT) {
                    ctx.close();
                    return;
                }
                ServletOutputStream sos = getStream();
                if(sos == null) {
                    log.warn("Stream null on non closed handler.");
                    return;
                }
                ByteBuf buf = backendResp.content();
                try {
                    byte arr[] = new byte[1024];
                    int end;
                    while((end = buf.readableBytes()) > 0) {
                        int len = Math.min(end, arr.length);
                        buf.readBytes(arr, 0, len);
                        sos.write(arr, 0, len);
                    }
                } finally {
                    buf.release();
                }
            }
        }

        private synchronized ServletOutputStream getStream() throws IOException {
            if(stream == null && !hasError) {
                stream = frontResp.getOutputStream();
            }
            return stream;
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            System.out.println("COMPLETE");
            Closeables.close(this.stream);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            this.hasError = true;
            log.error("Error in pipeline: ", cause);
            ctx.close();
            frontResp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, cause.toString());
        }
    }
}
