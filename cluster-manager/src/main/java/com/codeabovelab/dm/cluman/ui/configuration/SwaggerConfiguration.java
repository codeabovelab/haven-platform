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

package com.codeabovelab.dm.cluman.ui.configuration;

import com.google.common.base.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import javax.servlet.ServletContext;
import java.util.Date;

@Configuration
public class SwaggerConfiguration {

    @Autowired
    private ServletContext servletContext;

    @Bean
    public Docket newsApi() {
        Docket docket = new Docket(DocumentationType.SWAGGER_2);
        return docket
                .groupName("DockMaster")
                .apiInfo(apiInfo())
                //it need for correct samples of date
                .directModelSubstitute(Date.class, Long.class)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(makePathRegexp())
                .build();
    }

    private Predicate<String> makePathRegexp() {
        return PathSelectors.regex(".*");
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("DockMaster API")
                .description("Dockmaster is an open source Docker container management system. " +
                        "It integrates container, application, cluster, image, and registry management in one single place.")
                .contact(new Contact("Code Above Lab LLC", "https://github.com/codeabovelab/haven-platform", "info@codeabovelab.com"))
                .license("Apache License")
                .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
                .version("1.0")
                .build();
    }


}
