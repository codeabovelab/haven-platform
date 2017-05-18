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

package com.codeabovelab.dm.agent.infocol;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
{
        # cpu load 1.0 - 100%, .5 - 50% and etc (float)
        'cpuLoad': 0.0,
        # memory in bytes (float)
        'memory': {
            'total': 0.0,
            'available': 0.0,
            'used': 0.0
        },
        # disk usage for each mount point, in bytes, (float)
        'disks': {
            '/': {'total': 0.0, 'used': 0.0},
            '/home': {'total': 0.0, 'used': 0.0},
        },
        'net': {
            'eth0': {'bytesOut': 0.0, 'bytesIn': 0.0}
        }
    }
 */
@Data
public class Info {

    @Data
    public static class Disk {
        private float total;
        private float used;
    }

    @Data
    public static class Memory {
        private float total;
        private float used;
        private float available;
    }

    @Data
    public static class Net {
        private float bytesIn;
        private float bytesOut;
    }

    private float cpuLoad;
    private Memory memory;
    private final Map<String, Disk> disks = new HashMap<>();
    private final Map<String, Net> net = new HashMap<>();
}
