package com.codeabovelab.dm.cluman.cluster.compose;

import com.codeabovelab.dm.cluman.cluster.compose.model.ComposeArg;
import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfig;
import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerServiceImpl;
import com.codeabovelab.dm.cluman.model.NodeInfoProvider;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;
import org.springframework.web.client.AsyncRestTemplate;

import java.io.File;

import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ComposeExecutorTest.TestConfiguration.class)
public class ComposeExecutorTest {

    @Autowired
    ComposeExecutor composeExecutor;

    @Test
    @SuppressWarnings("unchecked")
    @Ignore
    public void testLaunchTask() throws Exception {
        ClusterConfig config = ClusterConfigImpl.builder().host("localhost:2375").build();
        DockerService dockerService = DockerServiceImpl.builder()
          .config(config)
          .restTemplate(new AsyncRestTemplate())
          .nodeInfoProvider(mock(NodeInfoProvider.class))
          .eventConsumer(mock(MessageBus.class))
          .objectMapper(new ObjectMapper())
          .cluster("test")
          .build();
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("docker-compose.yml").getFile());
        ComposeResult composeResult = composeExecutor.up(ComposeArg.builder().file(file).runUpdate(false).build(),
                dockerService);
        Assert.notNull(composeResult);

    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        ComposeExecutor composeExecutor() {
            return ComposeExecutor.builder().checkIntervalInSec(2).basedir("tmp").build();
        }
    }
}