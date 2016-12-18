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
import com.codeabovelab.dm.cluman.utils.FileUtils;
import com.codeabovelab.dm.common.utils.StringUtils;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

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

    private static final Pattern PATTERN = Pattern.compile("\\$[\\w]+\\$");
    private static final String HEADER = "X-Auth-Node";
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

    @RequestMapping(value = "/agent/{agentName:.*}", method = GET)
    public ResponseEntity<StreamingResponseBody> load(HttpServletRequest request,
                                                      @PathVariable("agentName") String agentName,
                                                      @RequestParam(value = "node", required = false) String node) {
        URL url = Thread.currentThread().getContextClassLoader().getResource("static/res/agent/node-agent.py");
        Assert.notNull(url);//if it happen then it a bug
        if(agentName == null) {
            agentName = "node-agent.py";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + FileUtils.encode(agentName));
        Replacer replacer = new Replacer(request, node);
        return new ResponseEntity<>((os) -> {
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                String string;
                while((string = reader.readLine()) != null) {
                    string = process(string, replacer);
                    os.write(string.getBytes(StandardCharsets.UTF_8));
                    // so we support only unix line break
                    os.write('\n');
                }
            }
        }, headers, HttpStatus.OK);
    }

    @AllArgsConstructor
    private class Replacer implements Function<String, String> {
        private final HttpServletRequest request;
        private final String node;

        @Override
        public String apply(String expr) {
            String res = null;
            switch (expr) {
                case "$MASTER$":
                    res = getServerAddress();
                    break;
                case "$SECRET$":
                    res = nodeSecret;
                    break;
                case "$DOCKER$":
                    res = node;
                    break;
            }
            return res == null? expr : res;
        }

        private String getServerAddress() {
            String name = request.getServerName() + ":" + request.getServerPort();
            if("localhost".equals(name)) {
                // it mean that server covered by proxy
                // also we may define server hostname in config
                name = null;
            }
            return name;
        }
    }

    private static String process(String string, Function<String, String> handler) {
        return StringUtils.replace(PATTERN, string, handler);
    }
}
