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

package com.codeabovelab.dm.common.cloud.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * Creates and fills RedisProperties from Service Discovery
 * If discoveryClient and cloud.redis.key property are not null tries to get connection params from SD
 *
 */
@Cloud
@Configuration
public class RedisConfig {

    private final static Logger LOGGER = LoggerFactory.getLogger(RedisConfig.class);

    public static final String REDIS_SERVICE_ID = "cloud.redis.key";

    @Autowired(required = false)
    private LoadBalancerClient loadBalancerClient;
    @Autowired
    private Environment environment;

    @Bean
    @Primary
    public RedisProperties dataSourceProperties() {
        RedisProperties properties = new RedisProperties();
        final String serviceId = environment.getProperty(REDIS_SERVICE_ID);
        if (loadBalancerClient != null && serviceId != null) {
            // also, we can choose redis sentinel system, or wait redis cluster (see https://jira.spring.io/browse/DATAREDIS-315)
            final ServiceInstance infos = loadBalancerClient.choose(serviceId);
            if (infos != null) {
                fillFields(infos, properties);
                LOGGER.info("registered redis from cloud {}:{}", infos.getHost(), infos.getPort());
            } else {
                LOGGER.warn("there is no services with id {} in service discovery", serviceId);
            }
        }
        return properties;
    }

    private void fillFields(ServiceInstance serviceInstance, RedisProperties properties) {
        properties.setHost(serviceInstance.getHost());
        properties.setPort(serviceInstance.getPort());
    }

}
