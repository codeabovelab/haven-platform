/*
 * Copyright 2017 Code Above Lab LLC
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

package com.codeabovelab.dm.cluman.ds;

import com.codeabovelab.dm.cluman.cluster.docker.ClusterConfig;
import com.codeabovelab.dm.cluman.cluster.docker.HttpAuthInterceptor;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerServiceEvent;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerServiceImpl;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.ds.nodes.NodeStorage;
import com.codeabovelab.dm.cluman.security.AccessContextFactory;
import com.codeabovelab.dm.cluman.security.DockerServiceSecurityWrapper;
import com.codeabovelab.dm.cluman.security.TempAuth;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.utils.Throwables;
import com.codeabovelab.dm.platform.http.async.NettyRequestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.AsyncRestTemplate;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 */
@Slf4j
@Component
public class DockerServiceFactory {

    @Autowired
    private RegistryRepository registryRepository;

    @Autowired
    @Qualifier(DockerServiceEvent.BUS)
    private MessageBus<DockerServiceEvent> dockerServiceEventMessageBus;

    @Autowired
    private NodeStorage nodeStorage;

    private final ExecutorService executor;

    @Autowired
    private AccessContextFactory aclContextFactory;

    @Autowired
    private ObjectMapper objectMapper;

    public DockerServiceFactory() {
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat(getClass().getSimpleName() + "-executor-%d")
          .setUncaughtExceptionHandler(Throwables.uncaughtHandler(log))
          .build());
    }

    public DockerService createDockerService(ClusterConfig clusterConfig, Consumer<DockerServiceImpl.Builder> dockerConsumer) {
        DockerServiceImpl.Builder b = DockerServiceImpl.builder();
        b.setConfig(clusterConfig);
        String cluster = clusterConfig.getCluster();
        if(cluster != null) {
            b.setCluster(cluster);
        }
        b.setRestTemplate(createNewRestTemplate());
        b.setEventConsumer(this::dockerEventConsumer);
        b.setNodeInfoProvider(nodeStorage);
        if (dockerConsumer != null) {
            dockerConsumer.accept(b);
        }
        b.setObjectMapper(objectMapper);
        DockerService ds = b.build();
        ds = securityWrapper(ds);
        return ds;
    }

    private void dockerEventConsumer(DockerServiceEvent dockerServiceEvent) {
        executor.execute(() -> {
            try(TempAuth auth = TempAuth.asSystem()) {
                dockerServiceEventMessageBus.accept(dockerServiceEvent);
            }
        });
    }

    private AsyncRestTemplate createNewRestTemplate() {
        // we use async client because usual client does not allow to interruption in some cases
        AsyncClientHttpRequestFactory factory = new NettyRequestFactory();
        final AsyncRestTemplate restTemplate = new AsyncRestTemplate(factory);
        restTemplate.setInterceptors(Collections.singletonList(new HttpAuthInterceptor(registryRepository)));
        return restTemplate;
    }

    public DockerService securityWrapper(DockerService dockerService) {
        return new DockerServiceSecurityWrapper(aclContextFactory, dockerService);
    }

    @PreDestroy
    private void preDestroy() {
        this.executor.shutdownNow();
    }

}
