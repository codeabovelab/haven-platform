package com.codeabovelab.dm.patform.configuration;

import com.codeabovelab.dm.platform.configuration.CacheConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@Import({CacheConfiguration.class})
@ComponentScan(basePackageClasses = {CacheConfigurationTest.class})
public class CacheConfigurationTest {
}
