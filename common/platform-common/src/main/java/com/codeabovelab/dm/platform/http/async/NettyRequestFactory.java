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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.*;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * We create our factory implementation based on {@link org.springframework.http.client.Netty4ClientHttpRequestFactory }
 * due to need consume of endless stream with "TransferEncoding: chunked", which default implementation does not allow.
 */
public class NettyRequestFactory implements ClientHttpRequestFactory,
  AsyncClientHttpRequestFactory, InitializingBean, DisposableBean {

    private final EventLoopGroup eventLoopGroup;

    private final boolean defaultEventLoopGroup;


    private SslContext sslContext;

    private int connectTimeout = -1;

    private int readTimeout = -1;

    private volatile Bootstrap bootstrap;


    /**
     * Create a new {@code Netty4ClientHttpRequestFactory} with a default
     * {@link NioEventLoopGroup}.
     */
    public NettyRequestFactory() {
        int ioWorkerCount = Runtime.getRuntime().availableProcessors() * 2;
        this.eventLoopGroup = new NioEventLoopGroup(ioWorkerCount);
        this.defaultEventLoopGroup = true;
    }

    /**
     * Create a new {@code Netty4ClientHttpRequestFactory} with the given
     * {@link EventLoopGroup}.
     * <p><b>NOTE:</b> the given group will <strong>not</strong> be
     * {@linkplain EventLoopGroup#shutdownGracefully() shutdown} by this factory;
     * doing so becomes the responsibility of the caller.
     */
    public NettyRequestFactory(EventLoopGroup eventLoopGroup) {
        Assert.notNull(eventLoopGroup, "EventLoopGroup must not be null");
        this.eventLoopGroup = eventLoopGroup;
        this.defaultEventLoopGroup = false;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Set the SSL context. When configured it is used to create and insert an
     * {@link io.netty.handler.ssl.SslHandler} in the channel pipeline.
     * <p>By default this is not set.
     */
    public void setSslContext(SslContext sslContext) {
        this.sslContext = sslContext;
    }

    /**
     * Set the underlying URLConnection's read timeout (in milliseconds).
     * A timeout value of 0 specifies an infinite timeout.
     * @see ReadTimeoutHandler
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    private Bootstrap getBootstrap() {
        if (this.bootstrap == null) {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(this.eventLoopGroup).channel(NioSocketChannel.class)
              .handler(new ChannelInitializer<SocketChannel>() {
                  @Override
                  protected void initChannel(SocketChannel channel) throws Exception {
                      configureChannel(channel.config());
                      ChannelPipeline pipeline = channel.pipeline();
                      if (sslContext != null) {
                          pipeline.addLast(sslContext.newHandler(channel.alloc()));
                      }
                      pipeline.addLast(new HttpClientCodec());
                      //pipeline.addLast(new HttpObjectAggregator(maxResponseSize));
                      if (readTimeout > 0) {
                          pipeline.addLast(new ReadTimeoutHandler(readTimeout,
                            TimeUnit.MILLISECONDS));
                      }
                  }
              });
            this.bootstrap = bootstrap;
        }
        return this.bootstrap;
    }

    /**
     * Template method for changing properties on the given {@link SocketChannelConfig}.
     * <p>The default implementation sets the connect timeout based on the set property.
     * @param config the channel configuration
     */
    protected void configureChannel(SocketChannelConfig config) {
        if (this.connectTimeout >= 0) {
            config.setConnectTimeoutMillis(this.connectTimeout);
        }
    }

    @Override
    public void afterPropertiesSet() {
        getBootstrap();
    }


    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
        return createRequestInternal(uri, httpMethod);
    }

    @Override
    public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
        return createRequestInternal(uri, httpMethod);
    }

    private NettyRequest createRequestInternal(URI uri, HttpMethod httpMethod) {
        return new NettyRequest(getBootstrap(), uri, httpMethod);
    }


    @Override
    public void destroy() throws InterruptedException {
        if (this.defaultEventLoopGroup) {
            // Clean up the EventLoopGroup if we created it in the constructor
            this.eventLoopGroup.shutdownGracefully().sync();
        }
    }

}

