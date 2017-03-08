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
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 */
@Slf4j
public class SourceUtil {

    private static final Splitter SP_VOLUMES = Splitter.on(':').limit(3);
    private static final Splitter SP_VOLUMES_OPTS = Splitter.on(',');
    private static final Splitter SP_HOSTS = Splitter.on(CharMatcher.anyOf(" \t"));


    public static void validateSource(RootSource source) {
        Assert.notNull(source, "source is null");
        String version = source.getVersion();
        Assert.isTrue(RootSource.V_1_0.equals(version), "source has unsupported version:" +
          version + " when support: " + RootSource.V_1_0);
    }

    public static void toSource(Service.ServiceSpec srvSpec, ServiceSource srv) {
        srv.setName(srvSpec.getName());
        Sugar.setIfNotNull(srv.getLabels()::putAll, srvSpec.getLabels());
        List<Endpoint.PortConfig> ports = srvSpec.getEndpointSpec().getPorts();
        if(ports != null) {
            ports.forEach(pc -> {
                srv.getPorts().add(new Port(pc.getTargetPort(), pc.getPublishedPort(), pc.getProtocol(), pc.getPublishMode()));
            });
        }

        Task.TaskSpec taskSpec = srvSpec.getTaskTemplate();
        ContainerSource cs = srv.getContainer();
        ContainerSpec conSpec = taskSpec.getContainer();
        toSource(conSpec, cs);
        Task.ResourceRequirements rrs = taskSpec.getResources();
        if(rrs != null) {
            TaskResources limits = rrs.getLimits();
            if(limits != null) {
                cs.setMemoryLimit(limits.getMemory());
            }
            TaskResources reserv = rrs.getReservations();
            if(reserv != null) {
                cs.setMemoryReservation(reserv.getMemory());
            }
        }
        Task.Placement placement = taskSpec.getPlacement();
        if(placement != null) {
            Sugar.setIfNotNull(srv.getConstraints()::addAll, placement.getConstraints());
        }

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

        ssb.taskTemplate(tsb.build());
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

    private static void toSource(ContainerSpec conSpec, ContainerSource cs) {
        hostsToSource(conSpec.getHosts(), cs.getExtraHosts());
        ContainerSpec.DnsConfig dc = conSpec.getDnsConfig();
        if(dc != null) {
            Sugar.setIfNotNull(cs.getDnsSearch()::addAll, dc.getSearch());
            Sugar.setIfNotNull(cs.getDns()::addAll, dc.getServers());
        }
        mountsToSource(conSpec.getMounts(), cs);
        String image = conSpec.getImage();
        ImageName in = ImageName.parse(image);
        cs.setImage(in.getFullName());
        cs.setImageId(in.getId());
        Sugar.setIfNotNull(cs.getLabels()::putAll, conSpec.getLabels());
        Sugar.setIfNotNull(cs.getCommand()::addAll, conSpec.getCommand());
        Sugar.setIfNotNull(cs.getEnvironment()::addAll, conSpec.getEnv());
        cs.setHostname(conSpec.getHostname());
    }

    private static void fromSource(ContainerSource cont, ContainerSpec.Builder csb) {
        csb.hosts(hostsFromSource(cont.getExtraHosts()));
        csb.dnsConfig(ContainerSpec.DnsConfig.builder()
          .servers(cont.getDns())
          .search(cont.getDnsSearch())
          .build());
        csb.mounts(convertMounts(cont));
        csb.image(ImageName.nameWithId(cont.getImage(), cont.getImageId()))
          .labels(cont.getLabels())
          .command(cont.getCommand())
          .env(cont.getEnvironment())
          .hostname(cont.getHostname());
    }

    @SuppressWarnings("deprecation")
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
                        case "rw":
                            mb.readonly(false);
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


    private static void mountsToSource(List<Mount> mounts, ContainerSource cs) {
        if(mounts == null) {
            return;
        }
        String driver = null;
        for(Mount mount: mounts) {
            Mount.Type type = mount.getType();
            if(type != Mount.Type.BIND && type != Mount.Type.VOLUME) {
                log.warn("Unsupported type: {} of mount {}", type, mount);
                continue;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(mount.getSource()).append(':').append(mount.getTarget()).append(':');
            appendOpt(sb, mount.isReadonly()? "ro" : "rw");
            Mount.VolumeOptions vo = mount.getVolumeOptions();
            if(vo != null) {
                Mount.Driver dc = vo.getDriverConfig();
                if(dc != null) {
                    String name = dc.getName();
                    if(name != null) {
                        if(driver != null) {
                            if(!name.equals(driver)) {
                                log.error("Unsupported, different volume drivers: {} and {} in one container: {}", name, driver, cs);
                                break;
                            }
                        } else {
                            driver = name;
                        }
                    }
                }
                if(vo.isNoCopy()) {
                    appendOpt(sb, "nocopy");
                }
            }
            Mount.BindOptions bo = mount.getBindOptions();
            if(bo != null) {
                Mount.Propagation propagation = bo.getPropagation();
                if(propagation != null) {
                    appendOpt(sb, propagation.name().toLowerCase());
                }
            }
            cs.getVolumeBinds().add(sb.toString());
        }
        cs.setVolumeDriver(driver);
    }

    private static void appendOpt(StringBuilder sb, String s) {
        int len = sb.length() - 1;
        if(len > 0 && sb.charAt(len) != ',') {
            sb.append(',');
        }
        sb.append(s);
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

    /**
     *
     * @param src lines of /etc/hosts file
     * @param dst pairs like 'name:ip'
     */
    private static void hostsToSource(List<String> src, List<String> dst) {
        if(src == null) {
            return;
        }
        src.forEach((hostLine) -> {
            // line in /etc/hosts file
            int sharpPos = hostLine.indexOf("#");
            if(sharpPos == 0) {
                // skip comments
                return;
            }
            String data = hostLine;
            if(sharpPos > 0) {
                data = hostLine.substring(0, sharpPos);
            }
            Iterator<String> i = SP_HOSTS.split(data).iterator();
            if(!i.hasNext()) {
                return;
            }
            String ip = i.next();
            while(i.hasNext()) {
                dst.add(i.next() + ":" + ip);
            }
        });
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
