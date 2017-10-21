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

import com.codeabovelab.dm.cluman.cluster.docker.model.Info;
import com.codeabovelab.dm.cluman.cluster.docker.model.InfoSwarm;
import com.codeabovelab.dm.cluman.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 */
@Slf4j
class DockerInfoParser {

    //an part of ugly swarm info DriverStatus entries
    private static final char NODE_ATTR_PREFIX = '└';
    private static final double KIB = 1024;
    private static final double MIB = KIB * 1024;
    private static final double GIB = MIB * 1024;
    private static final double TIB = GIB * 1024;
    private static final double PIB = TIB * 1024;


    private final Info info;
    private final DockerServiceInfo.Builder result;
    private boolean nodes = false;
    private NodeInfoImpl.Builder nib = null;
    private NodeMetrics.Builder nhb = null;

    private DockerInfoParser(Info info) {
        this.info = info;
        this.result = DockerServiceInfo.builder();
    }

    /**
     * Hack for using swarm API
     *
     * @param info
     * @return
     */
    static DockerServiceInfo.Builder parse(Info info) {
        DockerInfoParser parser = new DockerInfoParser(info);
        return parser.parse();
    }

    private static NodeMetrics.State parseState(String val) {
        if(val == null) {
            return null;
        }
        try {
            return NodeMetrics.State.valueOf(val.toUpperCase());
        } catch (Exception e) {
            //suppress
            return null;
        }
    }

    private void parseStatusList(List<List<String>> driverStatus) {
        final int size = driverStatus.size();
        for (int i = 0; i < size; i++) {
            final List<String> list = driverStatus.get(i);
            if(CollectionUtils.isEmpty(list)) {
                continue;
            }
            final String key = list.get(0);
            if(!nodes) {
                nodes = key.endsWith("Nodes");
                if(nodes) {
                    this.result.nodeCount(Integer.parseInt(list.get(1)));
                }
            } else {
                if (key.indexOf(NODE_ATTR_PREFIX) < 0) {//node name will start with usual letter
                    parseNodeHeader(list, key);
                } else {
                    try {
                        parseNodeAttr(list, key);
                    } catch(Exception e) {
                        log.error("Can not parse attr: {} at {} entry.", list, i, e);
                    }
                }
            }
        }
        endParsingCurrentNode();
    }

    private void parseNodeAttr(List<String> list, String key) {
        if(nhb == null) {
            nhb = NodeMetrics.builder();
            // we cannot use local time here because in TokeDiscoveryServer obtain time from node
            //nhb.setTime(LocalDateTime.now());
        }
        String val = list.get(1);
        int index = key.indexOf(NODE_ATTR_PREFIX) + 1;
        //skip spaces after PREFIX
        while(index < key.length() && key.charAt(index) == ' ') {
            index++;
        }
        switch (key.substring(index)) {
            case "Status": {
                NodeMetrics.State state = parseState(val);
                nhb.setState(state);
                nhb.setHealthy(state == NodeMetrics.State.HEALTHY);
                break;
            }
            case "Reserved CPUs": {
                String[] rt = org.springframework.util.StringUtils.split(val, "/");
                nhb.setSwarmCpusReserved(Integer.parseInt(rt[0].trim()));
                nhb.setSwarmCpusTotal(Integer.parseInt(rt[1].trim()));
                break;
            }
            case "Reserved Memory": {
                String[] rt = org.springframework.util.StringUtils.split(val, "/");
                nhb.setSwarmMemReserved(parseSize(rt[0].trim()));
                nhb.setSwarmMemTotal(parseSize(rt[1].trim()));
                break;
            }
        }
    }

    /**
     * Parse size from https://github.com/docker/go-units/blob/master/size.go
     * @param s
     * @return
     */
    private long parseSize(String s) {
        String[] pair = org.springframework.util.StringUtils.split(s, " ");
        double val = Double.parseDouble(pair[0]);
        double mult;
        switch (pair[1]) {
            case "KiB": mult = KIB; break;
            case "MiB": mult = MIB; break;
            case "GiB": mult = GIB; break;
            case "TiB": mult = TIB; break;
            case "PiB": mult = PIB; break;
            default: mult = 1d;
        }
        return (long) (val * mult);
    }

    private void parseNodeHeader(List<String> list, String key) {
        endParsingCurrentNode();
        nhb = null;
        String name = key.trim();
        if(name.indexOf('(') >= 0) {
            // this is ' (unknown)' node, which is temporary appeared in start process, and cause error, so we skip it
            return;
        }
        nib = NodeInfoImpl.builder().name(name);
        String address = list.get(1).trim();
        nib.address(address);
    }

    private void endParsingCurrentNode() {
        if(nib != null) {
            if(nhb != null) {
                nib.setHealth(nhb.build());
            }
            this.result.getNodeList().add(nib.build());
        }
    }

    private DockerServiceInfo.Builder parse() {
        log.debug("info {}", info);
        this.result.id(info.getId())
          .name(info.getName())
          .images(info.getImages())
          .ncpu(info.getNcpu())
          .memory(info.getMemory());
        ZonedDateTime systemTime = info.getSystemTime();
        if(systemTime != null) {
            this.result.setSystemTime(systemTime);
        }
        parseLabels();
        List<List<String>> statusList = info.getSystemStatus();
        if (statusList == null) {
            // old swarm use DriverStatus, but new - SystemStatus
            statusList = info.getDriverStatus();
        }
        if (statusList != null) {
            parseStatusList(statusList);
        }
        InfoSwarm swarm = info.getSwarm();
        if(swarm != null && StringUtils.hasText(swarm.getNodeId())) {
            result.setSwarm(convertSwarm(swarm));
            result.setNodeCount(swarm.getNodes());
        }
        return result;
    }

    private void parseLabels() {
        List<String> labels = info.getLabels();
        if(labels == null) {
            return;
        }
        Map<String, String> target = this.result.getLabels();
        labels.forEach(pair -> {
            String[] kv = StringUtils.split(pair, "=");
            if(kv != null) {
                target.put(kv[0], kv[1]);
            }
        });
    }

    private SwarmInfo convertSwarm(InfoSwarm src) {
        SwarmInfo.Builder sib = SwarmInfo.builder();
        String id = src.getCluster().getId();
        if(StringUtils.hasText(id)) {
            // docker can place empty string here
            sib.setClusterId(id);
        }
        // is not sure that it mean 'is manager'
        sib.setManager(src.isControlAvailable());
        sib.setNodeId(src.getNodeId());
        src.getRemoteManagers().forEach(rm -> sib.getManagers().add(rm.getAddress()));
        return sib.build();
    }
}
