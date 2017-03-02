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

package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.cluman.cluster.docker.model.Mount;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.ContainerSpec;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Endpoint;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Service;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.Task;
import com.codeabovelab.dm.cluman.model.ContainerService;
import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.codeabovelab.dm.cluman.model.ImageName;
import com.codeabovelab.dm.cluman.model.Port;
import com.google.common.base.Splitter;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * UI representation for Container service create.
 *
 * @see ContainerService
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UiContainerServiceCreate extends UiContainerServiceCore {

    private static final Splitter SP_VOLUMES = Splitter.on(':').limit(3);
    private static final Splitter SP_VOLUMES_OPTS = Splitter.on(',');

    @ApiModelProperty("service.version, need for update")
    protected long version;
    protected final List<Port> ports = new ArrayList<>();

    public Service.ServiceSpec.Builder toServiceSpec() {
        Service.ServiceSpec.Builder ssb = Service.ServiceSpec.builder();
        ssb.name(getName());
        ssb.labels(getLabels());
        ssb.endpointSpec(Endpoint.EndpointSpec.builder().build());
        ssb.taskTemplate(Task.TaskSpec.builder()
          .container(toContainerSpec(getContainer()))
          .build());
        return ssb;
    }

    private static ContainerSpec toContainerSpec(ContainerSource c) {
        ContainerSpec.Builder csb = ContainerSpec.builder();
        csb.hosts(convertHosts(c.getExtraHosts()));
        csb.dnsConfig(ContainerSpec.DnsConfig.builder()
          .servers(c.getDns())
          .search(c.getDnsSearch())
          .build());
        csb.mounts(convertMounts(c));
        return csb.image(ImageName.nameWithId(c.getImage(), c.getImageId()))
          .labels(c.getLabels())
          .command(c.getCommand())
          .env(c.getEnvironment())
          .hostname(c.getHostname())
          .build();
    }

    private static List<Mount> convertMounts(ContainerSource c) {
        List<Mount> res = new ArrayList<>();
        final String volumeDriver = c.getVolumeDriver();
        c.getVolumeBinds().forEach(vb -> {
            Iterator<String> i = SP_VOLUMES.split(vb).iterator();
            Mount.Builder mb = Mount.builder();
            mb.source(i.next());
            mb.target(i.next());
            Mount.VolumeOptions.Builder vob = Mount.VolumeOptions.builder();
            if(i.hasNext()) {
                Mount.BindOptions.Builder bo = Mount.BindOptions.builder();
                for(String opt : SP_VOLUMES_OPTS.split(i.next())) {
                    switch (opt) {
                        case "ro":
                            mb.readonly(true);
                            break;
                        case "rshared":
                            bo.propagation(Mount.Propagation.RSHARED);
                            break;
                        case "rslave":
                            bo.propagation(Mount.Propagation.RSLAVE);
                            break;
                        case "rprivate":
                            bo.propagation(Mount.Propagation.RPRIVATE);
                            break;
                        case "shared":
                            bo.propagation(Mount.Propagation.SHARED);
                            break;
                        case "slave":
                            bo.propagation(Mount.Propagation.SLAVE);
                            break;
                        case "private":
                            bo.propagation(Mount.Propagation.PRIVATE);
                            break;
                        case "nocopy":
                            vob.noCopy(true);
                            break;
                    }
                }
                mb.bindOptions(bo.build());
            }
            if(volumeDriver != null) {
                vob.driverConfig(Mount.Driver.builder().name(volumeDriver).build());
            }
            mb.volumeOptions(vob.build());
            res.add(mb.build());
        });
        //c.getVolumesFrom() - not supported by services
        return res;
    }

    /**
     * The format of extra hosts on swarmkit is specified in:
     * http://man7.org/linux/man-pages/man5/hosts.5.html
     *    IP_address canonical_hostname [aliases...]
     * @param extraHosts host in 'name:ip' format
     * @return hosts in unix format
     */
    private static List<String> convertHosts(List<String> extraHosts) {
        if(extraHosts == null) {
            return null;
        }
        List<String> res = new ArrayList<>(extraHosts.size());
        extraHosts.forEach(src -> {
            String[] hi = StringUtils.split(src, ":");
            if(hi != null) {
                res.add(hi[1] + " " + hi[0]);
            }
        });
        return res;
    }
}
