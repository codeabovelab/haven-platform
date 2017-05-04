package com.codeabovelab.dm.cluman.configs.container;

import com.codeabovelab.dm.cluman.DockerServiceMock;
import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfig;
import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfigImpl;
import com.codeabovelab.dm.cluman.cluster.docker.HttpAuthInterceptor;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerServiceImpl;
import com.codeabovelab.dm.cluman.cluster.filter.FilterFactory;
import com.codeabovelab.dm.cluman.ds.SwarmAdapterConfiguration;
import com.codeabovelab.dm.cluman.ds.clusters.DiscoveryStorageImpl;
import com.codeabovelab.dm.common.kv.InMemoryKeyValueStorage;
import com.codeabovelab.dm.common.kv.KeyValueStorage;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.objprinter.ObjectPrinterFactory;
import com.codeabovelab.dm.cluman.source.SourceService;
import com.codeabovelab.dm.common.json.JacksonUtils;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.AsyncRestTemplate;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Collections;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(classes = ClusterConfigFetcherTest.Config.class)
public class ClusterConfigFetcherTest {
    private static final String REAL = "real";
    private static final String MOCK = "mock";


    @ComponentScan(basePackageClasses = {
      ObjectPrinterFactory.class,
      KvMapperFactory.class,
      DiscoveryStorageImpl.class,
      FilterFactory.class,
      SourceService.class
    })
    @Import({SwarmAdapterConfiguration.PrerequestConfiguration.class})
    @Configuration
    public static class Config {

        @Bean
        ObjectMapper objectMapper() {
            return JacksonUtils.objectMapperBuilder();
        }

        @Bean
        FormattingConversionService conversionService() {
            return new DefaultFormattingConversionService();
        }

        @Bean
        KeyValueStorage keyValueStorage() {
            return new InMemoryKeyValueStorage();
        }

        @Bean
        TextEncryptor textEncryptor() {
            return Encryptors.noOpText();
        }

        @Bean
        Validator validator() {
            return Validation.buildDefaultValidatorFactory().getValidator();
        }

        @Bean
        DockerServices dockerServices() {
            DockerService mock = new DockerServiceMock(DockerServiceInfo.builder().build());
            DockerService real = dockerService();
            DockerServices dses = mock(DockerServices.class);
            when(dses.getService(MOCK)).thenReturn(mock);
            when(dses.getService(REAL)).thenReturn(real);
            when(dses.getOrCreateCluster(anyObject(), anyObject())).thenReturn(mock);
            return dses;
        }

        @SuppressWarnings("unchecked")
        DockerServiceImpl dockerService() {
            ClusterConfig config = ClusterConfigImpl.builder().host("localhost:2375").build();
            AsyncRestTemplate restTemplate = new AsyncRestTemplate();
            restTemplate.setInterceptors(
                    Collections.singletonList(
                            new HttpAuthInterceptor(null)));
            return DockerServiceImpl.builder()
              .config(config)
              .cluster("test")
              .restTemplate(restTemplate)
              .nodeInfoProvider(mock(NodeInfoProvider.class))
              .eventConsumer(mock(MessageBus.class))
              .objectMapper(new ObjectMapper())
              .build();
        }

        @Bean
        NodeStorage nodeStorage() {
            return mock(NodeStorage.class);
        }

    }

    @Autowired
    private SourceService clusterConfigFetcher;
    @Autowired
    private DiscoveryStorage discoveryStorage;


    @Before
    public void before() {
        discoveryStorage.getOrCreateCluster(MOCK, null);
    }

    @Test
    @Ignore
    public void test() {
        RootSource clusterDataConfig = clusterConfigFetcher.getClusterSource(MOCK);
        Assert.assertNotNull(clusterDataConfig);
    }

}