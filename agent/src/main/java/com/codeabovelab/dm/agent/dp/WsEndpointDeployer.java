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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

/**
 */
@Slf4j
@Component
public class WsEndpointDeployer implements ServletContextInitializer {

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        ServerContainer container = (ServerContainer) servletContext.getAttribute(ServerContainer.class.getName());
        try {
            // adding annotated endpoint cause wrapping it by server, that is not good in our case
            ServerEndpointConfig.Builder secb = ServerEndpointConfig.Builder.create(WsProxy.class, "/containers/{container}/attach/ws");
            secb.configurator(new SpringConfigurator());
            container.addEndpoint(secb.build());
        } catch (Exception e) {
            log.error("Can not deploy", e);
        }
    }

    private class SpringConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        @SuppressWarnings("unchecked")
        public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
            return (T) beanFactory.createBean(endpointClass, AutowireCapableBeanFactory.AUTOWIRE_NO, true);
        }
    }
}
