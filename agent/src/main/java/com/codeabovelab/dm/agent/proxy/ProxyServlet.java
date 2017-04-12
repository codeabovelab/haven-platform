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

package com.codeabovelab.dm.agent.proxy;


import com.codeabovelab.dm.common.utils.Closeables;
import com.codeabovelab.dm.common.utils.Uuids;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Rest controller which serves proxy requests
 */
@Slf4j
public class ProxyServlet extends GenericServlet {

    @Autowired
    private Backend backend;

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;
        String id = Uuids.longUid();
        NettyHandler handler = null;
        try {
            String uri = Utils.reconstructUri(request);
            log.debug("{}: start {} {}", id, request.getMethod(), uri);
            ChannelFuture cf = backend.connect().sync();
            Channel channel = cf.channel();
            DefaultFullHttpRequest backendReq = buildRequest(id, request, uri);
            handler = new NettyHandler(id, request, response);
            channel.pipeline().addLast(handler);
            channel.writeAndFlush(backendReq).sync();
            channel.closeFuture().sync();
            log.debug("{}: end", id);
        } catch (Exception e) {
            log.error("{}: error in service(): ", id, e);
        } finally {
            Closeables.close(handler);
        }
    }

    private DefaultFullHttpRequest buildRequest(String id, HttpServletRequest request, String uri) throws IOException {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        DefaultFullHttpRequest br = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri);
        HttpHeaders bh = br.headers();
        Utils.copyHeaders(request, bh);
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
}
