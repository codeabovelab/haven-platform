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

import com.codeabovelab.dm.agent.notifier.NotifierData;
import com.codeabovelab.dm.agent.notifier.SysInfo;
import com.codeabovelab.dm.cluman.model.DiskInfo;
import com.codeabovelab.dm.cluman.model.NetIfaceCounter;
import com.codeabovelab.dm.cluman.model.NodeMetrics;
import com.codeabovelab.dm.cluman.security.TempAuth;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


/**
 * REST Service for swarm clients (Swarm discovery hub service).
 * Also prepare node agent script for specified node.
 */
@RestController
@RequestMapping({"/discovery"})
@Slf4j
public class DiscoveryNodeController {

    private final NodeStorage storage;
    private final String nodeSecret;
    private final String startString;

    @Autowired
    public DiscoveryNodeController(NodeStorage storage,
                                   @Value("${dm.agent.notifier.secret:}") String nodeSecret,
                                   @Value("${dm.agent.start}") String startString) {
        this.storage = storage;
        this.startString = startString;
        this.nodeSecret = Strings.emptyToNull(nodeSecret);
    }

    @RequestMapping(value = "/nodes/{name}", method = POST, consumes = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    public ResponseEntity<String> registerNodeFromAgent(@RequestBody NotifierData data,
                                                        @PathVariable("name") String name,
                                                        @RequestHeader(name = NotifierData.HEADER, required = false) String nodeSecret,
                                                        @RequestParam(value = "ttl", required = false) Integer ttl) {
        if (this.nodeSecret != null && !this.nodeSecret.equals(nodeSecret)) {
            return new ResponseEntity<>("Server required node auth, need correct value of '" + NotifierData.HEADER + "' header.", UNAUTHORIZED);
        }
        NodeMetrics health = createNodeHealth(data);
        if (ttl == null) {
            // it workaround, we must rewrite ttl system (it not used)
            ttl = Integer.MAX_VALUE;
        }
        try (TempAuth ta = TempAuth.asSystem()) {
            storage.updateNode(name, ttl, b -> {
                b.addressIfNeed(data.getAddress());
                b.mergeHealth(health);
            });
        }
        log.info("Update node {}", name);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private NodeMetrics createNodeHealth(NotifierData nad) {
        SysInfo system = nad.getSystem();
        NodeMetrics.Builder nhb = NodeMetrics.builder();
        nhb.setTime(nad.getTime());
        if (system != null) {
            SysInfo.Memory mem = system.getMemory();
            if (mem != null) {
                nhb.setSysMemAvail(mem.getAvailable());
                nhb.setSysMemTotal(mem.getTotal());
                nhb.setSysMemUsed(mem.getUsed());
            }
            Map<String, SysInfo.Disk> disks = system.getDisks();
            if (disks != null) {
                for (Map.Entry<String, SysInfo.Disk> disk : disks.entrySet()) {
                    SysInfo.Disk value = disk.getValue();
                    if (value == null) {
                        continue;
                    }
                    long used = value.getUsed();
                    nhb.addDisk(new DiskInfo(disk.getKey(), used, value.getTotal()));
                }
            }
            Map<String, SysInfo.Net> net = system.getNet();
            if (net != null) {
                for (Map.Entry<String, SysInfo.Net> nic : net.entrySet()) {
                    if (nic == null) {
                        continue;
                    }
                    SysInfo.Net nicValue = nic.getValue();
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

    @RequestMapping(value = "/agent/", method = GET)
    public String agent(HttpServletRequest request) {
        return StrSubstitutor.replace(startString,
                of("secret", nodeSecret == null ? "" : "-e \"dm_agent_notifier_secret=" + nodeSecret + "\"",
                        "server", getServerAddress(request)), "{", "}");
    }


    private String getServerAddress(HttpServletRequest request) {
        String name = request.getServerName() + ":" + request.getServerPort();
        if ("localhost".equals(name)) {
            // it mean that server covered by proxy
            // also we may define server hostname in config
            name = null;
        }
        return name;
    }

}
