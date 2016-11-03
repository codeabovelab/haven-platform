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

package com.codeabovelab.dm.cluman.cluster.docker.management;

import com.codeabovelab.dm.cluman.cluster.compose.model.ApplicationEvent;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.mb.MessageBuses;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerEventConfig {

    @Bean(name = DockerServiceEvent.BUS)
    public MessageBus<DockerServiceEvent> dockerMessageBus() {
        return MessageBuses.create(DockerServiceEvent.BUS, DockerServiceEvent.class);
    }


    @Bean(name = ApplicationEvent.BUS)
    public MessageBus<ApplicationEvent> applicationMessageBus() {
        return MessageBuses.create(ApplicationEvent.BUS, ApplicationEvent.class);
    }
}
