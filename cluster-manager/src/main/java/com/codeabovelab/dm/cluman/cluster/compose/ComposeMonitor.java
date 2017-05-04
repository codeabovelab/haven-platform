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

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.common.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.observers.Observers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ComposeMonitor {

    private final BehaviorSubject<ResultCode> monitor;
    private final DockerService dockerService;
    private final Observer<Long> observer;
    private final String fileName;

    public ComposeMonitor(DockerService dockerService, String fileName) {
        this.monitor = BehaviorSubject.create();
        this.dockerService = dockerService;
        this.fileName = fileName;
        this.observer = Observers.create(t -> {
            List<String> containerIds = getContainerIds();
            containerIds.forEach(s -> {
                ContainerDetails details = dockerService.getContainer(s);
                log.debug("get container {}", details);
                if (checkContainer(details)) {
                    log.error("Container crashed {}", details);
                    monitor.onNext(ResultCode.ERROR);
                    monitor.onCompleted();
                    return;
                }
            });
        });
    }

    private boolean checkContainer(ContainerDetails details) {
        return details != null && !details.getState().isRunning() && details.getState().getExitCode() != 0;
    }

    public void subscribeToChanges(Action1<ResultCode> action, int checkIntervalInSec) {
        monitor.subscribeOn(Schedulers.newThread()).subscribe(action);
        Observable.interval(checkIntervalInSec, TimeUnit.SECONDS).takeUntil(t -> monitor.hasCompleted()).subscribe(observer);
    }

    public void stopMonitoring() {
        observer.onCompleted();
    }

    public List<String> getContainerIds() {
        String list = CommandBuilder.getContainerIds(fileName);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            int exitCode = ProcessUtils.executeCommand(list, null, outputStream, null, null, buildDockerEnv());
            if (exitCode == 0 && outputStream.toString() != null) {
                String commandOutput = outputStream.toString();
                return parseListCommandOutput(commandOutput);
            }
        } catch (Exception e) { log.error("", e); }
        return Collections.emptyList();
    }

    private List<String> parseListCommandOutput(String output) {
        List<String> containersIds = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(output, "\n");
        while (tokenizer.hasMoreTokens()) {
            containersIds.add(tokenizer.nextToken().trim());
        }
        return containersIds;
    }

    public Map<String, String> buildDockerEnv() {
        return env(dockerService);
    }

    public static Map<String, String> env(DockerService dockerService) {
        return Collections.singletonMap("DOCKER_HOST", "tcp://" + dockerService.getClusterConfig().getHost());
    }
}
