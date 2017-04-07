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
import com.google.common.collect.Iterators;
import io.netty.bootstrap.Bootstrap;
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
        String id = Uuids.longUid();
        NettyHandler handler = null;
        try {
            String uri = reconstructUri(request);
            log.debug("{}: start {} {}", id, request.getMethod(), uri);
            ChannelFuture cf = bootstrap.connect(new DomainSocketAddress("/var/run/docker.sock")).sync();
            Channel channel = cf.channel();
            handler = new NettyHandler(id, request, response);
            channel.pipeline().addLast(handler);
            DefaultFullHttpRequest backendReq = buildRequest(id, request, uri);
            channel.writeAndFlush(backendReq).sync();
            channel.closeFuture().sync();
            log.debug("{}: end", id);
        } catch (Exception e) {
            log.error("{}: error in service(): ", id, e);
        } finally {
            Closeables.close(handler);
        }
    }

    private String reconstructUri(HttpServletRequest request) {
        String q = request.getQueryString();
        String req = request.getRequestURI();
        if(q == null) {
            return req;
        }
        return req + "?" + q;
    }

    private DefaultFullHttpRequest buildRequest(String id, HttpServletRequest request, String uri) throws IOException {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        DefaultFullHttpRequest br = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri);
        Enumeration<String> headers = request.getHeaderNames();
        HttpHeaders bh = br.headers();
        while(headers.hasMoreElements()) {
            String header = headers.nextElement();
            Iterable<String> iter = () -> Iterators.forEnumeration(request.getHeaders(header));
            bh.add(header, iter);
        }
        int len = request.getContentLength();
        if(len > 0) {
            try(ServletInputStream is = request.getInputStream()) {
                br.content().writeBytes(is, len);
            }
        } else if(len == -1) {
            log.warn("{}: content length: -1", id);
        }
        return br;
    }

    @Override
    public void destroy() {
        group.shutdownGracefully();
    }

}
