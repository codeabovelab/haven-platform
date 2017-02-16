/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.cluman.cluster.application;

import com.codeabovelab.dm.cluman.cluster.compose.ComposeExecutor;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.ds.kv.etcd.EtcdConfiguration;
import com.codeabovelab.dm.cluman.model.Application;
import com.codeabovelab.dm.cluman.model.ApplicationImpl;
import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import com.codeabovelab.dm.cluman.source.ContainerSourceFactory;
import com.codeabovelab.dm.common.json.JacksonConfiguration;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ApplicationServiceTest.TestConfiguration.class)
@TestPropertySource(properties = {
        "dm.kv.etcd.urls=http://127.0.0.1:2379"})
@Ignore
public class ApplicationServiceTest {
    @Autowired
    private ApplicationService applicationService;

    @Test
    public void test() throws Exception {
        ApplicationImpl application = ApplicationImpl.builder()
                .name(UUID.randomUUID().toString())
                .cluster("cluster")
                .creatingDate(new Date())
                .containers(Arrays.asList("1", "2")).build();
        applicationService.addApplication(application);
        Application storedApp = applicationService.getApplication(application.getCluster(), application.getName());
        assertEquals(application, storedApp);
    }

    @EnableAutoConfiguration
    @Configuration
    @Import({EtcdConfiguration.class, JacksonConfiguration.class})
    public static class TestConfiguration {

        @Bean
        ContainerSourceFactory containerSourceFactory(ObjectMapper objectMapper) {
            return new ContainerSourceFactory(objectMapper);
        }

        @Bean
        @Autowired
        @SuppressWarnings("unchecked")
        ApplicationService applicationService(KvMapperFactory mapper, ContainerSourceFactory srcService) {
            DiscoveryStorage dockerServiceRegistry = mock(DiscoveryStorage.class);
            DockerService dockerService = mock(DockerService.class);
            when(dockerServiceRegistry.getService(anyString())).thenReturn(dockerService);
            when(dockerService.getContainer(anyString())).thenReturn(mock(ContainerDetails.class));
            ApplicationService applicationService = new ApplicationServiceImpl(mapper,
                    dockerServiceRegistry, mock(ComposeExecutor.class),
                    srcService,
                    mock(MessageBus.class));
            return applicationService;

        }
    }

}