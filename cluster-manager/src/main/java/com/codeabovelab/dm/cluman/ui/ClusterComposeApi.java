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

package com.codeabovelab.dm.cluman.ui;

import com.codeabovelab.dm.cluman.cluster.application.ApplicationService;
import com.codeabovelab.dm.cluman.cluster.compose.ComposeExecutor;
import com.codeabovelab.dm.cluman.cluster.compose.ComposeResult;
import com.codeabovelab.dm.cluman.cluster.compose.ComposeUtils;
import com.codeabovelab.dm.cluman.cluster.compose.model.ComposeArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerConfig;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.model.ApplicationImpl;
import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import com.google.common.io.Files;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 */
@RestController
@Slf4j
@RequestMapping(value = "/ui/api", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ClusterComposeApi {

    private final DiscoveryStorage discoveryStorage;
    private final ComposeExecutor composeExecutor;
    private final ApplicationService applicationService;

    @RequestMapping(value = "clusters/{cluster}/compose", method = POST, consumes = {MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ComposeResult> deployClusterFromCompose(@PathVariable("cluster") String cluster,
                                                                  @RequestPart(value = "data") MultipartFile multipartFile) throws Exception {
        //String root, String cluster, String app, String fileName
        File file = ComposeUtils.clusterPath(composeExecutor.getBasedir(), cluster, multipartFile.getName());
        Files.write(multipartFile.getBytes(), file);
        DockerService service = discoveryStorage.getService(cluster);
        ComposeResult composeResult = composeExecutor.up(ComposeArg.builder().file(file).build(), service);
        log.info("result of executing compose: {}", composeResult);

        createApplications(composeResult, cluster);
        return new ResponseEntity<>(composeResult, UiUtils.toStatus(composeResult.getResultCode()));
    }

    private void createApplications(ComposeResult composeResult, String cluster) {
        List<ContainerDetails> containers = composeResult.getContainerDetails();
        if(containers == null) {
            log.warn("Null list of containers from compose for cluster: '{}'", cluster);
            return;
        }
        Map<String, List<String>> collect = new HashMap<>();
        for(ContainerDetails container: containers) {
            ContainerConfig config = container.getConfig();
            if(config == null) {
                log.warn("Container '{}' without config for cluster: '{}'", container, cluster);
                continue;
            }
            Map<String, String> labels = config.getLabels();
            if(labels == null) {
                log.warn("Container '{}' without labels for cluster: '{}'", container, cluster);
                continue;
            }
            String app = labels.get(ApplicationService.APP_LABEL);
            if (StringUtils.hasText(app)) {
                collect.computeIfAbsent(app, (appName) -> new ArrayList<>()).add(container.getId());
            }
        }

        if (!collect.isEmpty()) {
            for (Map.Entry<String, List<String>> app : collect.entrySet()) {

                ApplicationImpl application = ApplicationImpl.builder()
                  .creatingDate(new Date())
                  .name(app.getKey())
                  .cluster(cluster)
                  .containers(Collections.unmodifiableList(app.getValue())).build();

                applicationService.addApplication(application);
            }
        }
    }
}
