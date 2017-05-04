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

package com.codeabovelab.dm.common.log;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;

/**
 *
 *  LogbackConfigurationListener checks 'logging.config.src' property after refreshing context
 *  and invokes reconfiguring logback
 *
 */
@Component
@AllArgsConstructor
public class LogbackConfigurationListener implements ApplicationListener<ApplicationEvent> {

    private final Environment environment;

    private static final Logger LOG = LoggerFactory.getLogger(LogbackConfigurationListener.class);

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        final String settings = environment.getProperty("logging.config.src");
        if (StringUtils.hasText(settings)) {
            try {
                final ContextBase context = (ContextBase) StaticLoggerBinder.getSingleton().getLoggerFactory();
                final JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                LOG.info("try to update logback configuration to {}", settings);
                context.reset();
                configurator.doConfigure(new ByteArrayInputStream(settings.getBytes()));
            } catch (JoranException e) {
                LOG.error("can't load settings", e);
            }
        }
    }
}
