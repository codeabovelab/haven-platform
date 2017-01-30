package com.codeabovelab.dm.cluman.bootstrap;

import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import static java.util.Collections.singletonMap;

public class ConfigServerBootstrapApplicationListener implements ApplicationListener<ApplicationPreparedEvent> {

    private final PropertySource<?> propertySource = new MapPropertySource(
            "configServer", singletonMap("spring.cloud.config.server.bootstrap", "true"));

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        ConfigurableEnvironment environment = event.getApplicationContext().getEnvironment();
        if (StringUtils.hasText(environment.resolvePlaceholders("${dm.config.git.uri:}"))) {
            if (!environment.getPropertySources().contains(this.propertySource.getName())) {
                environment.getPropertySources().addLast(this.propertySource);
            }
        }
    }

}
