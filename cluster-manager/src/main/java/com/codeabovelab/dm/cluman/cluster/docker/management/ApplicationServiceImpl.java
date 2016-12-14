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

import com.codeabovelab.dm.cluman.cluster.compose.ComposeExecutor;
import com.codeabovelab.dm.cluman.cluster.compose.ComposeResult;
import com.codeabovelab.dm.cluman.cluster.compose.model.ApplicationEvent;
import com.codeabovelab.dm.cluman.cluster.compose.model.ComposeArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.StopContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.CreateApplicationResult;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.source.ContainerSourceFactory;
import com.codeabovelab.dm.cluman.model.ApplicationSource;
import com.codeabovelab.dm.cluman.ds.DockerServiceRegistry;
import com.codeabovelab.dm.common.kv.KeyValueStorage;
import com.codeabovelab.dm.common.kv.WriteOptions;
import com.codeabovelab.dm.cluman.model.Application;
import com.codeabovelab.dm.cluman.model.ApplicationInstance;
import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.codeabovelab.dm.cluman.model.Severity;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

@Service
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private final KeyValueStorage keyValueStorage;
    private final DockerServiceRegistry dockerServiceRegistry;
    private final String appPrefix;
    private final ComposeExecutor composeExecutor;

    private final ObjectMapper objectMapper;
    private final MessageBus<ApplicationEvent> applicationBus;
    private final ContainerSourceFactory sourceService;

    @Autowired
    public ApplicationServiceImpl(KeyValueStorage keyValueStorage, ObjectMapper objectMapper,
                                  DockerServiceRegistry dockerServiceRegistry,
                                  ComposeExecutor composeExecutor,
                                  ContainerSourceFactory sourceService,
                                  @Qualifier(ApplicationEvent.BUS) MessageBus<ApplicationEvent> applicationBus) {
        this.keyValueStorage = keyValueStorage;
        this.dockerServiceRegistry = dockerServiceRegistry;
        this.appPrefix = keyValueStorage.getPrefix() + "/applications/";
        this.objectMapper = objectMapper;
        this.composeExecutor = composeExecutor;
        this.applicationBus = applicationBus;
        this.sourceService = sourceService;
    }

    @Override
    public List<Application> getApplications(String cluster) {
        Map<String, String> appsStrings = keyValueStorage.map(appPrefix + cluster);
        if(appsStrings == null) {
            return Collections.emptyList();
        }
        return appsStrings.values().stream().map(str -> {
            try {
                return objectMapper.readValue(str, ApplicationInstance.class);
            } catch (Exception e) {
                LOG.error("can't parse Applications", e);
                return null;
            }
        }).collect(Collectors.toList());
    }

    private ApplicationInstance readApplication(String cluster, String appId) {
        try {
            String value = keyValueStorage.get(buildKey(cluster, appId)).getValue();
            return objectMapper.readValue(value, ApplicationInstance.class);
        } catch (Exception e) {
            LOG.error("can't parse Applications", e);
            return null;
        }
    }

    @Override
    public CreateApplicationResult deployCompose(ComposeArg composeArg) throws Exception {

        DockerService service = dockerServiceRegistry.getService(composeArg.getClusterName());

        return upCompose(composeArg, service);
    }

    private CreateApplicationResult upCompose(ComposeArg composeArg, DockerService service) throws Exception {
        log.debug("about to launch {} at {}", composeArg, service.getCluster());
        fireStartEvent(composeArg);
        ComposeResult composeResult = composeExecutor.up(composeArg, service);
        log.info("result of {} : {}", composeArg, composeResult);
        fireEndEvent(composeResult, composeArg);
        List<ContainerDetails> containerDetails = firstNonNull(composeResult.getContainerDetails(), Collections.emptyList());

        ApplicationInstance application = ApplicationInstance.builder()
                .creatingDate(new Date())
                .initFile(composeArg.getFile().getCanonicalPath())
                .name(composeArg.getAppName())
                .cluster(composeArg.getClusterName())
                .containers(Collections.unmodifiableList(containerDetails.stream()
                        .map(a -> a.getId()).collect(Collectors.toList()))).build();

        addApplication(application);
        CreateApplicationResult createApplicationResult = new CreateApplicationResult();
        createApplicationResult.setApplication(application);
        createApplicationResult.setCode(composeResult.getResultCode());
        return createApplicationResult;

    }

    @Override
    public void startApplication(String cluster, String id) throws Exception {
        DockerService service = dockerServiceRegistry.getService(cluster);
        Application application = getApplication(cluster, id);

        if (application.getInitFile() != null) {
            // starting using compose, also checks new versions
            ComposeArg composeArg = ComposeArg.builder().appName(id).file(new File(application.getInitFile()))
                    .runUpdate(true).clusterName(cluster).build();
            upCompose(composeArg, service);
        } else {
            // starting manually
            application.getContainers().forEach(c -> service.startContainer(c));
        }
    }

    private void fireStartEvent(ComposeArg composeArg) {
        ApplicationEvent.Builder ae = ApplicationEvent.builder();
        ae.setAction("starting");
        ae.setSeverity(Severity.INFO);
        ae.setApplicationName(composeArg.getAppName());
        ae.setFileName(composeArg.getFile().getName());
        ae.setClusterName(composeArg.getClusterName());
        applicationBus.accept(ae.build());
    }

    private void fireEndEvent(ComposeResult composeResult, ComposeArg composeArg) {
        ApplicationEvent.Builder ae = ApplicationEvent.builder();
        ae.setAction("started");
        ae.setSeverity(composeResult.getResultCode() == ResultCode.OK ? Severity.INFO : Severity.ERROR);
        ae.setApplicationName(composeResult.getAppName());
        ae.setFileName(composeArg.getFile().getName());
        ae.setClusterName(composeArg.getClusterName());
        if (!CollectionUtils.isEmpty(composeResult.getContainerDetails())) {
            ae.setContainers(composeResult.getContainerDetails().stream().map(c -> c.getId()).collect(Collectors.toList()));
        }
        applicationBus.accept(ae.build());
    }

    @Override
    public void stopApplication(String cluster, String id) {
        Application application = getApplication(cluster, id);

        DockerService service = dockerServiceRegistry.getService(application.getCluster());
        application.getContainers().forEach(c -> service.stopContainer(StopContainerArg.builder().id(c).build()));
    }

    @Override
    public Application getApplication(String cluster, String appId) {
        ApplicationInstance applicationInstance = readApplication(cluster, appId);
        ExtendedAssert.notFound(applicationInstance, "application was not found " + appId);
        DockerService service = dockerServiceRegistry.getService(cluster);
        ApplicationInstance.ApplicationInstanceBuilder clone = applicationInstance.cloneToBuilder();
        List<String> existedContainers = applicationInstance.getContainers().stream()
                .filter(c -> service.getContainer(c) != null).collect(Collectors.toList());
        ApplicationInstance result = clone.containers(existedContainers).build();
        return result;
    }

    @Override
    public void addApplication(Application application) throws Exception {
        Assert.notNull(application, "application can't be null");
        String appName = application.getName();
        ExtendedAssert.matchId(appName, "application name");

        DockerService service = dockerServiceRegistry.getService(application.getCluster());
        List<String> containers = application.getContainers();
        List<String> existedContainers = containers.stream().filter(c -> service.getContainer(c) != null).collect(Collectors.toList());
        Assert.isTrue(!CollectionUtils.isEmpty(existedContainers), "Application doesn't have containers " + application);
//        Assert.isTrue(existedContainers.size() == containers.size());
        String value = objectMapper.writeValueAsString(application);
        keyValueStorage.set(buildKey(application.getCluster(), appName), value, WriteOptions.builder()
                .failIfExists(false).failIfAbsent(false).build());
    }

    @Override
    public void removeApplication(String cluster, String id) throws Exception {
        log.info("about to remove application: {}, in cluster: {}", id, cluster);
        Application application = getApplication(cluster, id);
        DockerService service = dockerServiceRegistry.getService(application.getCluster());
        composeExecutor.rm(application, service);
        keyValueStorage.delete(buildKey(cluster, id), WriteOptions.builder().build());

    }

    @Override
    public ApplicationSource getSource(String cluster, String appId) {
        Application application = getApplication(cluster, appId);
        DockerService service = dockerServiceRegistry.getService(cluster);
        ApplicationSource src = new ApplicationSource();
        src.setName(application.getName());
        application.getContainers().stream()
            .map(c -> {
                ContainerDetails cd = service.getContainer(c);
                ContainerSource conf = new ContainerSource();
                sourceService.toSource(cd, conf);
                conf.setApplication(appId);
                conf.setCluster(cluster);
                conf.getLabels().put(APP_LABEL, appId);
                return conf;
            })
            .forEach(src.getContainers()::add);
        return src;
    }

    @Override
    public File getInitComposeFile(String cluster, String appId) {

        ApplicationInstance applicationInstance = readApplication(cluster, appId);
        ExtendedAssert.notFound(applicationInstance, "application was not found " + appId);
        File file = new File(applicationInstance.getInitFile());
        ExtendedAssert.notFound(file.exists(), "can't find file for " + appId);
        return file;
    }

    private String buildKey(String cluster, String id) {
        return appPrefix + cluster + "/" + id;
    }

}
