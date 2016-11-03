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

package com.codeabovelab.dm.gateway.proxy.common;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.httpasyncclient.InstrumentedNHttpClientBuilder;
import com.codahale.metrics.httpclient.HttpClientMetricNameStrategy;
import com.codahale.metrics.httpclient.InstrumentedHttpClients;
import com.codeabovelab.dm.common.utils.Closeables;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.SmartApplicationListener;

import java.net.URISyntaxException;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Creates and configures required beans
 */
@Configuration
public class BalancerConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(BalancerConfiguration.class);

    @Autowired
    private MetricRegistry metricRegistry;

    private HttpProxy proxyInstance;

    /**
     * Sync or async client: important for nio pools
     */
    @Value("${balancer.client.async:true}")
    private Boolean async;

    /**
     * Enables HttpAsyncClient metrics
     */
    @Value("${balancer.metrics.enabled:true}")
    private Boolean metricsEbanled;

    /**
     * Max count of connections of proxy server
     */
    @Value("${balancer.max.connections:10000}")
    private Integer maxConnections;

    /**
     * Determines the timeout in milliseconds until a connection is established.
     * A timeout value of zero is interpreted as an infinite timeout.
     * <p>
     * A timeout value of zero is interpreted as an infinite timeout.
     * A negative value is interpreted as undefined (system default).
     * </p>
     * <p>
     * Default: {@code 2 seconds!}
     * </p>
     */
    @Value("${balancer.connect.timeout:2000}")
    private Integer connectTimeout;


    /**
     * Defines the socket timeout ({@code SO_TIMEOUT}) in milliseconds,
     * which is the timeout for waiting for data  or, put differently,
     * a maximum period inactivity between two consecutive data packets).
     * <p>
     * A timeout value of zero is interpreted as an infinite timeout.
     * A negative value is interpreted as undefined (system default).
     * </p>
     * <p>
     * Default: {@code 60 seconds!}
     * </p>
     */
    @Value("${balancer.socket.timeout:60000}")
    private Integer socketTimeout;

    /**
     * HttpClientMetricNameStrategies which contains only URL
     */
    private static final HttpClientMetricNameStrategy QUERYLESS_URL =
            (name, request) -> {
                try {
                    final URIBuilder url = new URIBuilder(request.getRequestLine().getUri());
                    return name(HttpClient.class, name, url.removeQuery().build().toString());
                } catch (URISyntaxException e) {
                    LOG.error("can't parse URL", e);
                    return name(HttpClient.class, name);
                }
            };

    /**
     * Creates and configures HttpProxy
     */
    @Bean
    HttpProxy httpProxy() {
        ProxyClient proxyClient = null;
        if (async) {
            if (metricsEbanled) {
                //Building HTTP Async client wrapper for gathering metrics
                proxyClient = new AsyncProxyClient(configuredHttpAsyncClient(new InstrumentedNHttpClientBuilder(metricRegistry, QUERYLESS_URL)));
                LOG.info("metrics enabled");
            } else {
                //Building Default HTTP Async client
                LOG.warn("metrics disabled");
                proxyClient = new AsyncProxyClient(configuredHttpAsyncClient(HttpAsyncClients.custom()));
            }
        } else {
            if (metricsEbanled) {
                //Building HTTP sync client wrapper for gathering metrics
                proxyClient = new SyncProxyClient(configuredHttpClient(InstrumentedHttpClients.custom(metricRegistry, QUERYLESS_URL)));
                LOG.info("metrics enabled");
            } else {
                //Building Default HTTP sync client
                LOG.warn("metrics disabled");
                proxyClient = new SyncProxyClient(configuredHttpClient(HttpClients.custom()));
            }
        }
        return proxyInstance = new HttpProxy(proxyClient);
    }

    protected CloseableHttpAsyncClient configuredHttpAsyncClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
        LOG.info("HttpAsyncClient settings: maxConnections: {}, socketTimeout: {}, connectTimeout: {}",
                maxConnections, socketTimeout, connectTimeout);
        return httpAsyncClientBuilder.setMaxConnPerRoute(maxConnections)
                .setMaxConnTotal(maxConnections)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setSocketTimeout(socketTimeout)
                        .setConnectTimeout(connectTimeout)
                        .build())
                .disableCookieManagement()
                .build();
    }

    protected CloseableHttpClient configuredHttpClient(HttpClientBuilder httpClientBuilder) {
        LOG.info("HttpClient settings: maxConnections: {}, socketTimeout: {}, connectTimeout: {}",
                maxConnections, socketTimeout, connectTimeout);
        return httpClientBuilder.setMaxConnPerRoute(maxConnections)
                .setMaxConnTotal(maxConnections)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setSocketTimeout(socketTimeout)
                        .setConnectTimeout(connectTimeout)
                        .build())
                .disableCookieManagement()
                .build();
    }

    /**
     * Listens ContextClosedEvent and Closes all async http connections
     */
    @Bean
    ApplicationListener<?> applicationListener() {
        return new SmartApplicationListener() {
            @Override
            public int getOrder() { return 0; }

            @Override
            public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
                return ContextClosedEvent.class.isAssignableFrom(eventType);
            }

            @Override
            public boolean supportsSourceType(Class<?> sourceType) { return true; }

            @Override
            public void onApplicationEvent(ApplicationEvent event) { Closeables.close(proxyInstance); }
        };
    }

}
