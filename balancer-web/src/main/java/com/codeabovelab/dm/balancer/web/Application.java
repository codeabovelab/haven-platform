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

package com.codeabovelab.dm.balancer.web;

import com.codeabovelab.dm.balancer.web.proxy.ProxyController;
import com.codeabovelab.dm.balancer.web.proxy.RibbonConfiguration;
import com.codeabovelab.dm.gateway.proxy.common.BalancerConfiguration;
import com.codeabovelab.dm.gateway.proxy.common.HttpProxy;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.servlet.Servlet;

@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class})
@Import({BalancerConfiguration.class, RibbonConfiguration.class, BalancerConfiguration.class})
@ComponentScan(basePackageClasses = {BalancerConfiguration.class, RibbonConfiguration.class})
@Configuration
public class Application extends SpringBootServletInitializer {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).run(args);
    }

    @Bean
    public Servlet dispatcherServlet(HttpProxy httpProxy, SpringClientFactory springClientFactory) {
        return new ProxyController(httpProxy, springClientFactory);
    }

}
