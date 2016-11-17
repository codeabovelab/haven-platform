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

package com.codeabovelab.dm.cluman.cluster.compose;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ComposeConfiguration {

    @Value("${dm.compose.monitor.check.interval.sec:2}")
    private int checkInterval;

    @Value("${dm.compose.files.location}")
    private String baseDir;

    @Bean
    ComposeExecutor composeExecutor() {
        ComposeExecutor composeExecutor = ComposeExecutor.builder()
                .basedir(baseDir)
                .checkIntervalInSec(checkInterval)
                .build();
        return composeExecutor;
    }
}
