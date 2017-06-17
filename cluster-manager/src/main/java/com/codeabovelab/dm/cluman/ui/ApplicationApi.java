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
import com.codeabovelab.dm.cluman.cluster.application.CreateApplicationResult;
import com.codeabovelab.dm.cluman.cluster.compose.ComposeExecutor;
import com.codeabovelab.dm.cluman.cluster.compose.ComposeUtils;
import com.codeabovelab.dm.cluman.cluster.compose.model.ComposeArg;
import com.codeabovelab.dm.cluman.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.springframework.http.MediaType.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping(value = "/ui/api/application", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ApplicationApi {

    private final ComposeExecutor composeExecutor;
    private final ApplicationService applicationService;
    private final DiscoveryStorage discoveryStorage;
    private final ObjectMapper mapper;

    @RequestMapping(value = "{cluster}/{appId}/compose/mp", method = POST, consumes = {MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Application> uploadComposeFile(@PathVariable("cluster") String cluster,
                                                         @PathVariable("appId") String appId,
                                                         @RequestPart(value = "file") MultipartFile multipartFile) throws Exception {
        //String root, String cluster, String app, String fileName
        File file = ComposeUtils.applicationPath(composeExecutor.getBasedir(), cluster, appId, null, true);
        Files.write(multipartFile.getBytes(), file);

        return launchComposeFile(file, cluster, appId);
    }

    private ResponseEntity<Application> launchComposeFile(File file, String cluster, String appId) throws Exception {

        CreateApplicationResult createApplicationResult = applicationService
                .deployCompose(ComposeArg.builder().file(file).runUpdate(true).clusterName(cluster).appName(appId).build());

        return new ResponseEntity<>(createApplicationResult.getApplication(), UiUtils.toStatus(createApplicationResult.getCode()));
    }

    @RequestMapping(value = "{cluster}/{appId}/compose", method = POST, consumes = {APPLICATION_OCTET_STREAM_VALUE})
    public ResponseEntity<Application> uploadComposeFileAsStream(@PathVariable("cluster") String cluster, @PathVariable("appId") String appId,
                                                                 @RequestBody InputStreamResource resource) throws Exception {
        try (InputStream inputStream = resource.getInputStream()) {
            File file = ComposeUtils.applicationPath(composeExecutor.getBasedir(), cluster, appId, null, true);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            Files.write(buffer, file);
            return launchComposeFile(file, cluster, appId);
        }

    }

    @RequestMapping(value = "{cluster}/{appId}/add", method = PUT)
    public void addApplication(@PathVariable("cluster") String cluster, @PathVariable("appId") String appId,
                               @RequestParam("containers") List<String> containers) {
        ApplicationImpl application = ApplicationImpl.builder()
                .creatingDate(new Date())
                .name(appId)
                .cluster(cluster)
                .containers(Collections.unmodifiableList(containers)).build();
        applicationService.addApplication(application);
    }

    @RequestMapping(value = "{cluster}/{appId}", method = GET)
    public Application getApplication(@PathVariable("cluster") String cluster, @PathVariable("appId") String appId) {
        return applicationService.getApplication(cluster, appId);
    }

    @RequestMapping(value = "{cluster}/{appId}", method = DELETE)
    public void deleteApplication(@PathVariable("cluster") String cluster, @PathVariable("appId") String appId) {
        applicationService.removeApplication(cluster, appId);
    }

    @RequestMapping(value = "{cluster}/{appId}/start", method = POST)
    public void startApplication(@PathVariable("cluster") String cluster, @PathVariable("appId") String appId) throws Exception {
        applicationService.startApplication(cluster, appId);
    }

    @RequestMapping(value = "{cluster}/{appId}/stop", method = POST)
    public void stopApplication(@PathVariable("cluster") String cluster, @PathVariable("appId") String appId) {
        applicationService.stopApplication(cluster, appId);
    }

    @RequestMapping(value = "{cluster}/all", method = GET)
    public List<Application> applicationList(@PathVariable("cluster") String cluster) {
        return applicationService.getApplications(cluster);
    }

    @RequestMapping(value = "{cluster}/{appId}/source", method = GET)
    public ApplicationSource getSource(@PathVariable("cluster") String cluster, @PathVariable("appId") String appId) {
        return applicationService.getSource(cluster, appId);
    }

    @RequestMapping(value = "{cluster}/{appId}/initFile", method = GET, produces = APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getInitFile(@PathVariable("cluster") String cluster, @PathVariable("appId") String appId) {
        File initComposeFile = applicationService.getInitComposeFile(cluster, appId);
        log.info("fetching config file for application: {}, cluster: {}, path {}", appId, cluster, initComposeFile);
        if (!initComposeFile.exists()) {
            throw new NotFoundException("file not found " + initComposeFile);
        }
        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.set("Content-Disposition", "attachment; filename=" + initComposeFile.getName());
        respHeaders.setContentLength(initComposeFile.length());

        return new ResponseEntity<>(new PathResource(initComposeFile.toPath()), respHeaders, HttpStatus.OK);
    }
}
