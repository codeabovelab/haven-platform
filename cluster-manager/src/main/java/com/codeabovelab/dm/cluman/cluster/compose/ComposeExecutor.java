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

import com.codeabovelab.dm.cluman.cluster.compose.model.ComposeArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.StopContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.model.Application;
import com.codeabovelab.dm.common.utils.ProcessUtils;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Execute specified compose file in specified dockerService
 */
@Slf4j
@Builder
public class ComposeExecutor {

    private final int checkIntervalInSec;
    private final String basedir;

    /**
     * Executes specified compose file in specified cluster
     *
     * @param composeArg
     * @return ComposeResult
     */
    public ComposeResult up(final ComposeArg composeArg, DockerService dockerService) {
        Assert.notNull(composeArg.getFile(), "file can't be null");
        String path = composeArg.getFile().getAbsolutePath();
        log.info("Running file", composeArg, path);
        ComposeMonitor monitor = new ComposeMonitor(dockerService, path);
        if (composeArg.isRunUpdate()) {
            updateImages(path, monitor);
        }
        if (composeArg.isCheckContainersUpDuringStart()) {
            monitor.subscribeToChanges(r -> shutDown(monitor, path, dockerService), checkIntervalInSec);
        }
        return startCompose(monitor, path, composeArg.getAppName(), dockerService);
    }

    public void stop(Application application, final DockerService dockerService) {
        if (checkFile(application.getInitFile())) {
            stopTask(application.getInitFile(), ComposeMonitor.env(dockerService));
        }
    }

    public void rm(Application application, final DockerService dockerService) {
        if (checkFile(application.getInitFile())) {
            rmTask(application.getInitFile(), ComposeMonitor.env(dockerService));
        }
    }

    private void updateImages(String path, ComposeMonitor monitor) {
        String pullCommand = CommandBuilder.pullImages(path);
        int imageUpdateExitCode = ProcessUtils.executeCommand(pullCommand, null, monitor.buildDockerEnv());
        if (imageUpdateExitCode != 0) {
            log.error("unable to update images");
        }
    }

    private ComposeResult startCompose(final ComposeMonitor monitor, String path, final String appName, final DockerService dockerService) {
        int exitCode = ProcessUtils.executeCommand(CommandBuilder.launchTask(path), null, monitor.buildDockerEnv());
        monitor.stopMonitoring();
        if (exitCode == 0) {
            List<String> containerIds = monitor.getContainerIds();
            List<ContainerDetails> res = containerIds.stream().map(s -> dockerService.getContainer(s)).collect(toList());
            log.info("compose file: {} successfully executed, result: {}", path, res);
            ComposeResult.ComposeResultBuilder result = ComposeResult.builder().appName(appName).containerDetails(res);
            if (CollectionUtils.isEmpty(res)) {
                return result.resultCode(ResultCode.NOT_MODIFIED).build();
            }
            return result.resultCode(ResultCode.OK).build();
        } else {
            shutDown(monitor, path, dockerService);
            return ComposeResult.builder().resultCode(ResultCode.ERROR).appName(appName).build();
        }
    }

    /**
     * Stop containers via docker-compose
     *
     * @param path    file.getAbsolutePath()
     * @param monitor
     */
    private void shutDown(ComposeMonitor monitor, String path, final DockerService dockerService) {
        int exitCode = stopTask(path, monitor.buildDockerEnv());
        if (exitCode != 0) {
            shutDownManually(monitor.getContainerIds(), dockerService);
        }
    }

    private int stopTask(String path, Map<String, String> env) {
        log.info("run shutdown for compose execution of file", path);
        String stopTask = CommandBuilder.stopTask(path);
        return ProcessUtils.executeCommand(stopTask, null, env);
    }

    private int rmTask(String path, Map<String, String> env) {
        log.info("run rm for compose execution of file", path);
        String stopTask = CommandBuilder.downTask(path);
        return ProcessUtils.executeCommand(stopTask, null, env);
    }

    /**
     * Stop all containers manually
     */
    private void shutDownManually(List<String> containerIds, final DockerService dockerService) {
        containerIds.forEach(id -> {
            ServiceCallResult serviceCallResult = dockerService.stopContainer(StopContainerArg.builder().id(id).build());
            ResultCode code = serviceCallResult.getCode();
            if (code == ResultCode.ERROR) {
                log.error("can't stop container {}", id);
            }
        });
    }

    public String getBasedir() {
        return basedir;
    }

    private boolean checkFile(String initFile) {
        if (initFile == null || !new File(initFile).exists()) {
            log.error("skipping - file doesn't exist {}", initFile);
            return false;
        }
        return true;
    }
}
