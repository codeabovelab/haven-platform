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

package com.codeabovelab.dm.cluman.source;

import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.cluster.docker.model.Mount;
import com.codeabovelab.dm.cluman.cluster.docker.model.swarm.*;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.common.utils.Sugar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 */
@Slf4j
public class SourceUtil {

    public static void validateSource(RootSource source) {
        Assert.notNull(source, "source is null");
        String version = source.getVersion();
        Assert.isTrue(RootSource.V_1_0.equals(version), "source has unsupported version:" +
          version + " when support: " + RootSource.V_1_0);
    }


    public static void fromSource(ServiceSource srv, Service.ServiceSpec.Builder ssb) {
        ssb.name(srv.getName());
        ssb.labels(srv.getLabels());
        Endpoint.EndpointSpec.Builder esb = Endpoint.EndpointSpec.builder();
        srv.getPorts().forEach(p -> {
            esb.port(Endpoint.PortConfig.builder()
              .publishedPort(p.getPublicPort())
              .targetPort(p.getPrivatePort())
              .protocol(p.getType())
              .publishMode(p.getMode())
              .build());
        });
        ssb.endpointSpec(esb.build());

        Task.TaskSpec.Builder tsb = Task.TaskSpec.builder();
        ContainerSource cont = srv.getContainer();
        ContainerSpec.Builder csb = ContainerSpec.builder();
        fromSource(cont, csb);
        tsb.container(csb.build());

        Task.ResourceRequirements.Builder rrsb = Task.ResourceRequirements.builder();
        rrsb.limits(toTaskResources(cont.getMemoryLimit(), cont.getCpuQuota()));
        rrsb.reservations(toTaskResources(cont.getMemoryReservation(), cont.getCpuPeriod()));
        tsb.resources(rrsb.build());

        tsb.placement(Task.Placement.builder().constraints(srv.getConstraints()).build());

        networksFromSource(tsb, cont);

        ssb.taskTemplate(tsb.build());
    }

    private static void networksFromSource(Task.TaskSpec.Builder tsb, ContainerSource cont) {
        List<String> nets = new ArrayList<>();
        {
            String mainNet = cont.getNetwork();
            if(mainNet != null) {
                nets.add(mainNet);
            }
        }
        nets.addAll(cont.getNetworks());
        if(nets.isEmpty()) {
            return;
        }
        tsb.networks(nets.stream()
          .filter(net -> !"default".equals(net))
          .map(net -> new SwarmNetwork.NetworkAttachmentConfig(net, Collections.emptyList()))
          .collect(Collectors.toList()));
    }

    private static TaskResources toTaskResources(Long mem, Integer cpu) {
        if(mem == null && cpu == null) {
            return null;
        }
        TaskResources.Builder trb = TaskResources.builder();
        if(mem != null) {
            trb.memory(mem);
        }
        if(cpu != null) {
            trb.nanoCPUs(cpu * 1000L);
        }
        return trb.build();
    }

    private static void fromSource(ContainerSource cont, ContainerSpec.Builder csb) {
        csb.hosts(hostsFromSource(cont.getExtraHosts()));
        csb.dnsConfig(ContainerSpec.DnsConfig.builder()
          .servers(cont.getDns())
          .search(cont.getDnsSearch())
          .build());
        csb.mounts(cont.getMounts().stream().map(SourceUtil::fromMountSource).collect(Collectors.toList()));
        csb.image(ImageName.nameWithId(cont.getImage(), cont.getImageId()))
          .labels(cont.getLabels())
          .command(cont.getEntrypoint())
          .args(cont.getCommand())
          .env(cont.getEnvironment())
          .hostname(cont.getHostname());
    }

    /**
     * The format of extra hosts on swarmkit is specified in:
     * http://man7.org/linux/man-pages/man5/hosts.5.html
     *    IP_address canonical_hostname [aliases...]
     * @param extraHosts host in 'name:ip' format
     * @return hosts in unix format
     */
    private static List<String> hostsFromSource(List<String> extraHosts) {
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

    public static Mount fromMountSource(MountSource m) {
        Mount.Type type = m.getType();
        Mount.Builder mb = Mount.builder();
        switch (type) {
            case BIND: {
                MountSource.BindSource bs = (MountSource.BindSource) m;
                mb.bindOptions(Mount.BindOptions.builder().propagation(bs.getPropagation()).build());
            }
            break;
            case TMPFS: {
                MountSource.TmpfsSource ts = (MountSource.TmpfsSource) m;
                mb.tmpfsOptions(Mount.TmpfsOptions.builder()
                  .mode(ts.getMode())
                  .size(ts.getSize())
                  .build());
            }
            break;
            case VOLUME: {
                MountSource.VolumeSource vs = (MountSource.VolumeSource) m;
                Mount.VolumeOptions.Builder vob = Mount.VolumeOptions.builder();
                vob.driverConfig(Mount.Driver.builder()
                  .name(vs.getDriver())
                  .options(vs.getDriverOpts())
                  .build());
                vob.labels(vs.getLabels());
                vob.noCopy(vs.isNoCopy());
                mb.volumeOptions(vob.build());
            }
            break;
            default:
                // for unsupported type
                return null;
        }
        mb.readonly(m.isReadonly());
        mb.source(m.getSource());
        mb.target(m.getTarget());
        mb.type(type);
        return mb.build();
    }

    public static MountSource toMountSource(Mount m) {
        Mount.Type type = m.getType();
        MountSource ms;
        switch (type) {
            case BIND: {
                MountSource.BindSource bs = new MountSource.BindSource();
                Mount.BindOptions bo = m.getBindOptions();
                if(bo != null) {
                    bs.setPropagation(bo.getPropagation());
                }
                ms = bs;
            }
            break;
            case TMPFS: {
                MountSource.TmpfsSource ts = new MountSource.TmpfsSource();
                Mount.TmpfsOptions to = m.getTmpfsOptions();
                if(to != null) {
                    ts.setMode(to.getMode());
                    ts.setSize(to.getSize());
                }
                ms = ts;
            }
            break;
            case VOLUME: {
                MountSource.VolumeSource vs = new MountSource.VolumeSource();
                Mount.VolumeOptions vo = m.getVolumeOptions();
                if(vo != null) {
                    Mount.Driver dc = vo.getDriverConfig();
                    if(dc != null) {
                        vs.setDriver(dc.getName());
                        Sugar.setIfNotNull(vs.getDriverOpts()::putAll, dc.getOptions());
                    }
                    Sugar.setIfNotNull(vs.getLabels()::putAll, vo.getLabels());
                    vs.setNoCopy(vo.isNoCopy());
                }
                ms = vs;
            }
            break;
            default:
                // for unsupported type
                return null;
        }
        ms.setReadonly(m.isReadonly());
        ms.setSource(m.getSource());
        ms.setTarget(m.getTarget());
        ms.setType(type);
        return ms;
    }

    public static MountSource toMountSource(ContainerDetails.MountPoint p) {
        Mount.Type type = p.getType();
        MountSource ms;
        switch (type) {
            case BIND: {
                MountSource.BindSource bs = new MountSource.BindSource();
                bs.setPropagation(p.getPropagation());
                ms = bs;
            }
            break;
            case TMPFS: {
                MountSource.TmpfsSource ts = new MountSource.TmpfsSource();
                // mount point does not provide enough info
                ms = ts;
            }
            break;
            case VOLUME: {
                MountSource.VolumeSource vs = new MountSource.VolumeSource();
                vs.setDriver(p.getDriver());
                ms = vs;
            }
            break;
            default:
                // for unsupported type
                return null;
        }
        ms.setReadonly(!p.isRw());
        ms.setSource(p.getSource());
        ms.setTarget(p.getDestination());
        ms.setType(type);
        return ms;
    }
}
