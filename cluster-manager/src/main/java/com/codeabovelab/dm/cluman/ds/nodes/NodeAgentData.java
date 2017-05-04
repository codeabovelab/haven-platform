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

import com.codeabovelab.dm.cluman.model.NodeMetrics;
import com.codeabovelab.dm.cluman.model.NodeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for node agent
 */
@Data
@SuppressWarnings("deprecation")
public class NodeAgentData implements NodeInfo {
    @Data
    public static class Tu {
        private long total;
        private long used;
    }

    /**
     * Network interface counter
     */
    @Data
    public static class Nic {
        private long bytesIn;
        private long bytesOut;
    }
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Tau extends Tu {
        private long available;
    }
    @Data
    public static class SystemStatus {
        private float cpuLoad;
        private Tau memory;
        private Map<String, Tu> disks;
        private Map<String, Nic> net;
    }

    private LocalDateTime time;
    private String id;
    private String name;
    private String address;
    private boolean on;
    private SystemStatus system;
    private final Map<String, String> labels = new HashMap<>();


    public String getAddress() {
        return address;
    }

    public NodeMetrics getHealth() {
        return null;
    }

    public String getCluster() {
        return null;
    }

    @Override
    public String getIdInCluster() {
        return null;
    }

    @Override
    public long getVersion() {
        return 0;
    }
}
