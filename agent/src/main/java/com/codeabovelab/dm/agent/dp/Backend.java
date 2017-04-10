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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 */
@Component
class Backend implements InitializingBean, DisposableBean {

    private Bootstrap bootstrap;
    private EpollEventLoopGroup group;

    @Override
    public void destroy() throws Exception {
        group.shutdownGracefully();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
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

    ChannelFuture connect() {
        return bootstrap.connect(new DomainSocketAddress("/var/run/docker.sock"));
    }
}
