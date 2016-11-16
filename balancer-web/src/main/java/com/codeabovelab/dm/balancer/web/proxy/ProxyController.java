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

package com.codeabovelab.dm.balancer.web.proxy;

import com.codeabovelab.dm.gateway.proxy.common.HttpProxy;
import com.codeabovelab.dm.gateway.proxy.common.HttpProxyContext;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerContext;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import rx.Observable;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Random;

import static com.codeabovelab.dm.balancer.web.proxy.RibbonConfiguration.SERVICEID;

/**
 * Rest controller which serves proxy requests
 */
public class ProxyController extends GenericServlet {

    private final Random random = new Random();
    private static final Logger LOG = LoggerFactory.getLogger(ProxyController.class);
    private final HttpProxy httpProxy;
    private final LoadBalancerCommand.Builder<Object> commandBuilder;

    /**
     * Ribbon requires not empty result from loadBalancerClient.execute for gathering correct statistics
     */
    private static final Observable STUB = Observable.just(new Object());


    @Autowired
    public ProxyController(HttpProxy httpProxy, SpringClientFactory springClientFactory) {
        this.httpProxy = httpProxy;

        RibbonLoadBalancerContext context = springClientFactory.getLoadBalancerContext(SERVICEID);
        IClientConfig clientConfig = springClientFactory.getClientConfig(SERVICEID);
        ILoadBalancer loadBalancer = springClientFactory.getLoadBalancer(SERVICEID);
        HttpClientLoadBalancerErrorHandler requestSpecificRetryHandler = getRequestSpecificRetryHandler(clientConfig);

        this.commandBuilder = LoadBalancerCommand.builder()
                .withRetryHandler(requestSpecificRetryHandler)
                .withLoadBalancerContext(context)
                .withClientConfig(clientConfig)
                .withLoadBalancer(loadBalancer);
    }


    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;
        /**
         * A command that is used to produce the Observable from the load balancer execution. The load balancer is responsible for
         * the following:
         * <ul>
         * <li>Choose a server</li>
         * <li>Retry on exception, controlled by {@link com.netflix.client.RetryHandler}</li>
         * <li>Provide feedback to the {@link com.netflix.loadbalancer.LoadBalancerStats}</li>
         * </ul>
         */
        commandBuilder.build().submit(instance -> {
            try {
                final URI uri = new URI("http", null, instance.getHost(), instance.getPort(), request.getRequestURI(), null, null);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("send request to {}", uri.toString());
                }
                final HttpProxyContext proxyContext = new HttpProxyContext(request,
                        response,
                        uri,
                        Long.toUnsignedString(random.nextLong(), 16) /*TODO Vitaly see history and remove this comment*/);
                httpProxy.service(proxyContext);
                return STUB;
            } catch (Exception e) {
                return Observable.error(e);
            }
        }).toBlocking().single();
    }

    public HttpClientLoadBalancerErrorHandler getRequestSpecificRetryHandler(
            IClientConfig config) {
        return new HttpClientLoadBalancerErrorHandler(config);
    }

}
