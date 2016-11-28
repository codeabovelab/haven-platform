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

package com.codeabovelab.dm.cluman.ds.nodes;

import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.security.TempAuth;
import com.codeabovelab.dm.cluman.ui.HttpException;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


/**
 * REST Service for swarm clients (Swarm discovery hub service)
 */
@RestController
@RequestMapping({"/swarm_token_discovery" /*deprecated*/, "/discovery"})
@Slf4j
public class DiscoveryNodeController {

    private final String HEADER = "X-Auth-Node";
    private final NodeStorage storage;
    private final String nodeSecret;

    @Autowired
    public DiscoveryNodeController(NodeStorage storage, @Value("${dm.nodesDiscovery.secret:}") String nodeSecret) {
        this.storage = storage;
        this.nodeSecret = Strings.emptyToNull(nodeSecret);
    }

    @RequestMapping(value = "/nodes/{name}", method = POST, consumes = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    public ResponseEntity<String> registerNodeFromAgent(@RequestBody NodeAgentData data,
                             @PathVariable("name") String name,
                             @RequestHeader(name = HEADER, required = false) String nodeSecret,
                             @RequestParam("ttl") int ttl) {
        if(this.nodeSecret != null && !this.nodeSecret.equals(nodeSecret)) {
            return new ResponseEntity<>("Server required node auth, need correct value of '" + HEADER + "' header.", HttpStatus.UNAUTHORIZED);
        }
        NodeInfoImpl.Builder builder = NodeInfoImpl.builder()
          .from(data)
          .name(name);
        builder.health(createNodeHealth(data));
        NodeInfo node = builder.build();
        try (TempAuth ta = TempAuth.asSystem()) {
            storage.updateNode(NodeUpdate.builder().node(node).build(), ttl);
        }
        log.info("Update node {}", node.getName());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private NodeMetrics createNodeHealth(NodeAgentData nad) {
        NodeAgentData.SystemStatus system = nad.getSystem();
        NodeMetrics.Builder nhb = NodeMetrics.builder();
        nhb.setTime(nad.getTime());
        if(system != null) {
            NodeAgentData.Tau mem = system.getMemory();
            if(mem != null) {
                nhb.setSysMemAvail(mem.getAvailable());
                nhb.setSysMemTotal(mem.getTotal());
                nhb.setSysMemUsed(mem.getUsed());
            }
            Map<String, NodeAgentData.Tu> disks = system.getDisks();
            if(disks != null) {
                for(Map.Entry<String, NodeAgentData.Tu> disk: disks.entrySet()) {
                    NodeAgentData.Tu value = disk.getValue();
                    if(value == null) {
                        continue;
                    }
                    long used = value.getUsed();
                    nhb.addDisk(new DiskInfo(disk.getKey(), used, value.getTotal()));
                }
            }
            Map<String, NodeAgentData.Nic> net = system.getNet();
            if(net != null) {
                for(Map.Entry<String, NodeAgentData.Nic> nic: net.entrySet()) {
                    if(nic == null) {
                        continue;
                    }
                    NodeAgentData.Nic nicValue = nic.getValue();
                    NetIfaceCounter counter = new NetIfaceCounter(nic.getKey(), nicValue.getBytesIn(), nicValue.getBytesOut());
                    nhb.addNet(counter);
                }
            }
            nhb.setSysCpuLoad(system.getCpuLoad());
        }

        //we can resolve healthy through analysis of disk and mem availability
        nhb.setHealthy(true);
        return nhb.build();
    }
}
