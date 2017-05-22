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

import com.codeabovelab.dm.agent.notifier.SysInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 */
public class NetCollector implements Collector {
    private final Path path;

    public NetCollector(InfoCollector ic) {
        this.path = Paths.get(ic.getRootPath(), "sys/class/net/");
    }

    @Override
    public void fill(SysInfo info) throws Exception {
        // /sys/class/net/eth0/statistics/rx_bytes
        // /sys/class/net/eth0/statistics/tx_bytes
        Map<String, SysInfo.Net> nets = info.getNet();
        for(Path devPath: (Iterable<Path>)(Files.list(path)::iterator)) {
            String dev = devPath.getFileName().toString();
            SysInfo.Net net = new SysInfo.Net();
            readNet(devPath, net);
            nets.put(dev, net);
        }
    }

    private void readNet(Path dev, SysInfo.Net net) throws IOException {
        long rx = readLong(dev.resolve("statistics/rx_bytes"));
        long tx = readLong(dev.resolve("statistics/tx_bytes"));
        net.setBytesIn(rx);
        net.setBytesOut(tx);
    }

    private long readLong(Path path) throws IOException {
        return Long.parseLong(Files.readAllLines(path).get(0));
    }
}
