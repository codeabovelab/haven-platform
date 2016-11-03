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

import com.google.common.collect.Lists;
import com.netflix.client.ClientException;
import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.config.IClientConfig;
import org.apache.http.ConnectionClosedException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * A handler that determines if an exception is retriable for load balancer,
 * and if an exception or error response should be treated as circuit related failures
 * so that the load balancer can avoid such server.
 *
 */
public class HttpClientLoadBalancerErrorHandler  extends DefaultLoadBalancerRetryHandler {

    @SuppressWarnings("unchecked")
    protected final List<Class<? extends Throwable>> retriable =
            Lists.newArrayList(UnknownHostException.class, ConnectException.class, SocketTimeoutException.class, ConnectTimeoutException.class,
                    NoHttpResponseException.class, ConnectionPoolTimeoutException.class, ConnectionClosedException.class, HttpHostConnectException.class);

    @SuppressWarnings("unchecked")
    protected final List<Class<? extends Throwable>> circuitRelated =
            Lists.newArrayList(UnknownHostException.class, SocketException.class, SocketTimeoutException.class, ConnectTimeoutException.class,
                    ConnectionClosedException.class, HttpHostConnectException.class);

    public HttpClientLoadBalancerErrorHandler() {
        super();
    }

    public HttpClientLoadBalancerErrorHandler(IClientConfig clientConfig) {
        super(clientConfig);
    }

    public HttpClientLoadBalancerErrorHandler(int retrySameServer,
                                              int retryNextServer, boolean retryEnabled) {
        super(retrySameServer, retryNextServer, retryEnabled);
    }

    /**
     * Test if an exception should be treated as circuit failure. For example,
     * a {@link ConnectException} is a circuit failure. This is used to determine
     * whether successive exceptions of such should trip the circuit breaker to a particular
     * host by the load balancer. If false but a server response is absent,
     * load balancer will also close the circuit upon getting such exception.
     * @return true if the Throwable has one of the following exception type as a cause:
     *
     * {@link SocketException}, {@link SocketTimeoutException}
     */
    @Override
    public boolean isCircuitTrippingException(Throwable e) {
        if (e instanceof ClientException) {
            return ((ClientException) e).getErrorType() == ClientException.ErrorType.SERVER_THROTTLED;
        }

        return super.isCircuitTrippingException(e);
    }


    /**
     * Test if an exception is retriable for the load balancer
     *
     * @param e the original exception
     * @param sameServer if true, the method is trying to determine if retry can be
     *        done on the same server. Otherwise, it is testing whether retry can be
     *        done on a different server
     */
    @Override
    public boolean isRetriableException(Throwable e, boolean sameServer) {
        if (e instanceof ClientException) {
            ClientException ce = (ClientException) e;
            if (ce.getErrorType() == ClientException.ErrorType.SERVER_THROTTLED) {
                return !sameServer && retryEnabled;
            }
        }
        return super.isRetriableException(e, sameServer);
    }

    @Override
    protected List<Class<? extends Throwable>> getRetriableExceptions() {
        return retriable;
    }

    @Override
    protected List<Class<? extends Throwable>> getCircuitRelatedExceptions() {
        return circuitRelated;
    }
}