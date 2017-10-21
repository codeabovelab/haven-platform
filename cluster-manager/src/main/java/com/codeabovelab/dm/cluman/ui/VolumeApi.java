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

package com.codeabovelab.dm.cluman.ui;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.DeleteUnusedVolumesArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetVolumesArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.RemoveVolumeArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.CreateVolumeCmd;
import com.codeabovelab.dm.cluman.cluster.docker.model.Volume;
import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.cluman.ui.model.UiVolume;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


/**
 */
@RestController
@RequestMapping(value = "/ui/api/volumes", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VolumeApi {

    private final DiscoveryStorage discoveryStorage;

    private DockerService getDocker(@RequestParam("cluster") String clusterName) {
        NodesGroup cluster = discoveryStorage.getCluster(clusterName);
        ExtendedAssert.notFound(cluster, "Can not find cluster: " + clusterName);
        return cluster.getDocker();
    }

    @RequestMapping(path = "/", method = RequestMethod.GET)
    public Collection<UiVolume> list(@RequestParam("cluster") String clusterName) {
        DockerService docker = getDocker(clusterName);
        List<Volume> volumes = docker.getVolumes(new GetVolumesArg());
        return volumes.stream().map(UiVolume::from).collect(Collectors.toList());
    }

    @RequestMapping(path = "/get", method = RequestMethod.GET)
    public UiVolume get(@RequestParam("cluster") String clusterName,
                        @RequestParam("volume") String volumeName) {
        DockerService docker = getDocker(clusterName);
        Volume volume = docker.getVolume(volumeName);
        ExtendedAssert.notFound(volume, "No volumes with name: " + volumeName);
        return UiVolume.from(volume);
    }

    @RequestMapping(path = "/create", method = RequestMethod.POST)
    public UiVolume create(@RequestParam("cluster") String clusterName,
                           @RequestBody UiVolume src) {
        DockerService docker = getDocker(clusterName);
        CreateVolumeCmd arg = new CreateVolumeCmd();
        arg.setName(src.getName());
        arg.setDriver(src.getDriver());
        arg.setDriverOpts(src.getOptions());
        arg.setLabels(src.getLabels());
        Volume volume = docker.createVolume(arg);
        ExtendedAssert.error(volume != null, "Create of volumes with name: " + src.getName() + " return null");
        return UiVolume.from(volume);
    }

    @RequestMapping(path = "/delete", method = RequestMethod.DELETE)
    public ResponseEntity<?> delete(@RequestParam("cluster") String clusterName,
                                 @RequestParam("volume") String volume,
                                 @RequestParam(value = "force", required = false) Boolean force) {
        DockerService docker = getDocker(clusterName);
        RemoveVolumeArg arg = new RemoveVolumeArg();
        arg.setName(volume);
        arg.setForce(force);
        ServiceCallResult scr = docker.removeVolume(arg);
        return UiUtils.createResponse(scr);
    }

    @RequestMapping(path = "/delete-unused", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteUnused(@RequestParam("cluster") String clusterName) {
        DockerService docker = getDocker(clusterName);
        DeleteUnusedVolumesArg arg = new DeleteUnusedVolumesArg();
        ServiceCallResult scr = docker.deleteUnusedVolumes(arg);
        return UiUtils.createResponse(scr);
    }
}
