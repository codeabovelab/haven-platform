package com.codeabovelab.dm.cluman.cluster.docker.management;

import com.codeabovelab.dm.cluman.cluster.compose.ComposeExecutor;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.ds.DockerServiceRegistry;
import com.codeabovelab.dm.cluman.ds.kv.etcd.EtcdConfiguration;
import com.codeabovelab.dm.cluman.model.Application;
import com.codeabovelab.dm.cluman.model.ApplicationInstance;
import com.codeabovelab.dm.cluman.source.ContainerSourceFactory;
import com.codeabovelab.dm.common.json.JacksonConfiguration;
import com.codeabovelab.dm.common.kv.KeyValueStorage;
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
        ApplicationInstance application = ApplicationInstance.builder()
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
        ApplicationService applicationService(KeyValueStorage keyValueStorage, ObjectMapper objectMapper, ContainerSourceFactory srcService) {
            DockerServiceRegistry dockerServiceRegistry = mock(DockerServiceRegistry.class);
            DockerService dockerService = mock(DockerService.class);
            when(dockerServiceRegistry.getService(anyString())).thenReturn(dockerService);
            when(dockerService.getContainer(anyString())).thenReturn(mock(ContainerDetails.class));
            ApplicationService applicationService = new ApplicationServiceImpl(keyValueStorage, objectMapper,
                    dockerServiceRegistry, mock(ComposeExecutor.class),
                    srcService,
                    mock(MessageBus.class));
            return applicationService;

        }
    }

}