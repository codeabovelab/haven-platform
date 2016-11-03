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

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableConfigurationProperties
public class RibbonConfiguration {

    public static final String SERVICEID = "dm-gateway-web";

    /**
     * Loads properties with prefix: DM-GATEWAY-WEB.ribbon
     * @return
     */
    @Bean
    public IClientConfig ribbonClientConfig() {
        DefaultClientConfigImpl config = new DefaultClientConfigImpl();
        config.loadProperties(SERVICEID);
        return config;
    }


    /**
     * A rule that skips servers with "tripped" circuit breaker and picks the
     * server with lowest concurrent requests.
     *
     */
    @Bean
    @Profile("bestAvailableRule")
    public IRule bestAvailableRule(IClientConfig config) {
        BestAvailableRule rule = new BestAvailableRule();
        rule.initWithNiwsConfig(config);
        return rule;
    }

    /**
     * A load balancer rule that filters out servers that:
     * <ul>
     * <li> are in circuit breaker tripped state due to consecutive connection or read failures, or</li>
     * <li> have active connections that exceeds a configurable limit (default is Integer.MAX_VALUE).</li>
     * property: niws.loadbalancer.availabilityFilteringRule.activeConnectionsLimit
     * </ul>
     *
     */
    @Bean
    @Profile("availabilityFilteringRule")
    public IRule availabilityFilteringRule(IClientConfig config) {
        AvailabilityFilteringRule rule = new AvailabilityFilteringRule();
        rule.initWithNiwsConfig(config);
        return rule;
    }

    /**
     * The most well known and basic loadbalacing strategy, i.e. Round Robin Rule.
     *
     */
    @Bean
    @Profile("roundRobinRule")
    public IRule roundRobinRule(IClientConfig config) {
        RoundRobinRule rule = new RoundRobinRule();
        rule.initWithNiwsConfig(config);
        return rule;
    }

    /**
     * Rule that use the average/percentile response times
     * to assign dynamic "weights" per Server which is then used in
     * the "Weighted Round Robin" fashion.
     * <p>
     * The basic idea for weighted round robin has been obtained from JCS
     * The implementation for choosing the endpoint from the list of endpoints
     * is as follows:Let's assume 4 endpoints:A(wt=10), B(wt=30), C(wt=40),
     * D(wt=20).
     * <p>
     * Using the Random API, generate a random number between 1 and10+30+40+20.
     * Let's assume that the above list is randomized. Based on the weights, we
     * have intervals as follows:
     * <p>
     * 1-----10 (A's weight)
     * <br>
     * 11----40 (A's weight + B's weight)
     * <br>
     * 41----80 (A's weight + B's weight + C's weight)
     * <br>
     * 81----100(A's weight + B's weight + C's weight + C's weight)
     * <p>
     * Here's the psuedo code for deciding where to send the request:
     * <p>
     * if (random_number between 1 &amp; 10) {send request to A;}
     * <br>
     * else if (random_number between 11 &amp; 40) {send request to B;}
     * <br>
     * else if (random_number between 41 &amp; 80) {send request to C;}
     * <br>
     * else if (random_number between 81 &amp; 100) {send request to D;}
     * <p>
     * When there is not enough statistics gathered for the servers, this rule
     * will fall back to use {@link RoundRobinRule}.
     */
    @Bean
    @Profile("weightedResponseTimeRule")
    public IRule weightedResponseTimeRule(IClientConfig config) {
        WeightedResponseTimeRule rule = new WeightedResponseTimeRule();
        rule.initWithNiwsConfig(config);
        return rule;
    }

}
