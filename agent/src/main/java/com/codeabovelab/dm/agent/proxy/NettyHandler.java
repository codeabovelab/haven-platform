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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 */
// TODO implement web socket & chunked front request streaming
@Slf4j
class NettyHandler extends ChannelInboundHandlerAdapter implements AutoCloseable {
    private final HttpServletResponse frontResp;
    private final HttpServletRequest frontReq;
    private volatile ServletOutputStream stream;
    private volatile boolean hasError;
    private final String id;

    NettyHandler(String id, HttpServletRequest frontReq, HttpServletResponse frontResp) {
        this.frontReq = frontReq;
        this.frontResp = frontResp;
        this.id = id;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof HttpResponse) {
            handleHeader((HttpResponse) msg);
        } else if (msg instanceof HttpContent) {
            handleContent(ctx, (HttpContent) msg);
        } else {
            log.debug("{}: receive unknown msg: {}", id, msg);
        }
    }

    private void handleContent(ChannelHandlerContext ctx, HttpContent backendResp) throws IOException {
        if (backendResp != LastHttpContent.EMPTY_LAST_CONTENT) {
            try {
                synchronized (frontResp) {
                    ServletOutputStream sos = getStream();
                    if (sos == null) {
                        log.warn("{}: Stream null on non closed handler.", id);
                        return;
                    }
                    ByteBuf buf = backendResp.content();
                    copy(buf, sos);
                    sos.flush();
                }
            } finally {
                backendResp.release();
            }
        }
        if (backendResp instanceof LastHttpContent) {
            log.debug("{}: receive last", id);
            ctx.close();
        }
    }

    private void copy(ByteBuf buf, ServletOutputStream sos) throws IOException {
        byte arr[] = new byte[1024];
        int end;
        while ((end = buf.readableBytes()) > 0) {
            int len = Math.min(end, arr.length);
            buf.readBytes(arr, 0, len);
            sos.write(arr, 0, len);
        }
    }

    private void handleHeader(HttpResponse backendResp) {
        HttpResponseStatus status = backendResp.status();
        synchronized (frontResp) {
            frontResp.setStatus(status.code());
            HttpHeaders headers = backendResp.headers();
            for (String name : headers.names()) {
                List<String> vals = headers.getAll(name);
                vals.forEach(val -> frontResp.addHeader(name, val));
            }
        }
    }

    private synchronized ServletOutputStream getStream() throws IOException {
        synchronized (frontResp) {
            if (stream == null && !hasError) {
                stream = frontResp.getOutputStream();
            }
            return stream;
        }
    }

    public void close() throws Exception {
        log.debug("{}: close handler", id);
        ServletOutputStream stream = this.stream;
        if (stream != null) {
            synchronized (frontResp) {
                try {
                    stream.flush();
                } catch (java.nio.channels.ClosedChannelException e) {
                    //nothing
                } catch (Exception e) {
                    log.error("{}: can not flush front stream: ", id, e);
                }
                try {
                    stream.close();
                } catch (IOException e) {
                    // stream.close() often throw exception when client close connection
                    // we do not logging this for reduce noise
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        try {
            this.hasError = true;
            log.error("{}: Error in pipeline: ", id, cause);
            ctx.close();
            synchronized (frontResp) {
                if (!frontResp.isCommitted()) {
                    frontResp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, cause.toString());
                }
            }
        } catch (Exception e) {
            log.error("{}: Error at exception caught: ", e);
        }
    }

    public String getId() {
        return id;
    }
}
