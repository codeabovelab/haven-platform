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
import com.codeabovelab.dm.cluman.utils.AddressUtils;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.utils.SSLUtil;
import com.codeabovelab.dm.common.utils.Throwables;
import com.codeabovelab.dm.platform.http.async.NettyRequestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkSslContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.AsyncRestTemplate;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 */
@Slf4j
@Component
public class DockerServiceFactory {

    private final RegistryRepository registryRepository;
    private final MessageBus<DockerServiceEvent> dockerServiceEventMessageBus;
    private final NodeStorage nodeStorage;
    private final ExecutorService executor;
    private final AccessContextFactory aclContextFactory;
    private final ObjectMapper objectMapper;

    @Value("${dm.ssl.check:true}")
    private boolean checkSsl;

    @Autowired
    public DockerServiceFactory(ObjectMapper objectMapper, AccessContextFactory aclContextFactory, NodeStorage nodeStorage,
                                @Qualifier(DockerServiceEvent.BUS) MessageBus<DockerServiceEvent> dockerServiceEventMessageBus,
                                RegistryRepository registryRepository) {
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat(getClass().getSimpleName() + "-executor-%d")
          .setUncaughtExceptionHandler(Throwables.uncaughtHandler(log))
          .build());
        this.objectMapper = objectMapper;
        this.aclContextFactory = aclContextFactory;
        this.nodeStorage = nodeStorage;
        this.dockerServiceEventMessageBus = dockerServiceEventMessageBus;
        this.registryRepository = registryRepository;
    }

    public DockerService createDockerService(ClusterConfig clusterConfig, Consumer<DockerServiceImpl.Builder> dockerConsumer) {
        DockerServiceImpl.Builder b = DockerServiceImpl.builder();
        b.setConfig(clusterConfig);
        String cluster = clusterConfig.getCluster();
        if(cluster != null) {
            b.setCluster(cluster);
        }
        b.setRestTemplate(createNewRestTemplate(AddressUtils.isHttps(clusterConfig.getHost())));
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

    private AsyncRestTemplate createNewRestTemplate(boolean ssl) {
        // we use async client because usual client does not allow to interruption in some cases
        NettyRequestFactory factory = new NettyRequestFactory();
        if(ssl) {
            try {
                SSLContext sslc = SSLContext.getInstance("TLS");
                if(!checkSsl) {
                    sslc.init(null, new TrustManager[]{new SSLUtil.NullX509TrustManager()}, null);
                }
                factory.setSslContext(new JdkSslContext(sslc, true, ClientAuth.OPTIONAL));
            } catch (GeneralSecurityException e) {
                log.error("", e);
            }
        }
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
