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

package com.codeabovelab.dm.cluman;

import com.codeabovelab.dm.cluman.configuration.SecurityConfiguration;
import com.codeabovelab.dm.cluman.events.EventsConfiguration;
import com.codeabovelab.dm.cluman.job.JobConfiguration;
import com.codeabovelab.dm.cluman.mail.MailConfiguration;
import com.codeabovelab.dm.cluman.objprinter.ObjectPrinterFactory;
import com.codeabovelab.dm.cluman.reconfig.AppConfigService;
import com.codeabovelab.dm.cluman.source.SourceService;
import com.codeabovelab.dm.cluman.users.UsersStorage;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.utils.AppInfo;
import com.codeabovelab.dm.platform.configuration.CacheConfiguration;
import com.codeabovelab.dm.platform.configuration.SecurityApplicationConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Class with main method
 * enables AutoConfiguration and Eureka server
 */
@EnableAutoConfiguration(exclude = EndpointWebMvcAutoConfiguration.class)
@Configuration
@EnableSwagger2
@Import({
        SecurityApplicationConfiguration.class, CacheConfiguration.class, JobConfiguration.class,
        MailConfiguration.class
})
// we use ComponentScan and do not use personal subpackage for app therefore we get ugly thing below
@ComponentScan(basePackages = {"com.codeabovelab.dm.cluman.cluster",
        "com.codeabovelab.dm.cluman.ui", "com.codeabovelab.dm.cluman.ds", "com.codeabovelab.dm.cluman.pipeline",
        "com.codeabovelab.dm.cluman.configs", "com.codeabovelab.dm.common.security"},
        basePackageClasses = {
                SecurityConfiguration.class,
                EventsConfiguration.class,
                AppConfigService.class,
                ObjectPrinterFactory.class,
                SourceService.class,
                UsersStorage.class,
                KvMapperFactory.class
        })
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@Slf4j
public class Application {

    public static void main(String[] args) {
        log.info("Application Version: {}, revision: {}, Build time: {}", AppInfo.getApplicationVersion(),
                AppInfo.getBuildRevision(), AppInfo.getBuildTime());
        SpringApplication.run(Application.class, args);
    }

}
