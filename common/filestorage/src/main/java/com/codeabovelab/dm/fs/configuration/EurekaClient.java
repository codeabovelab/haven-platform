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

package com.codeabovelab.dm.fs.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.instrument.classloading.LoadTimeWeaver;

import javax.annotation.PostConstruct;

/**
 * Eureka client configuration.
 */
@EnableEurekaClient
@EnableDiscoveryClient
@Configuration
@Profile("eureka")
@Import({EurekaDiscoveryClientConfiguration.class})
public class EurekaClient implements LoadTimeWeaverAware {

    @Autowired
    private EurekaDiscoveryClientConfiguration discoveryClientConfiguration;

    @PostConstruct
    private void postConstruct() {
        // it`s workaround which force eureka client to start
        discoveryClientConfiguration.start();
    }

    @Override
    public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
        //it need only for early startup of configuration
    }
}
