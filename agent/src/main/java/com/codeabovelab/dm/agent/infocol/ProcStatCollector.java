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

import java.io.BufferedReader;
import java.io.File;
import java.util.Iterator;
import static com.codeabovelab.dm.agent.infocol.InfoUtils.*;

/**
 */
class ProcStatCollector implements Collector, Refreshable {
    private static class Data {
        final long user;
        final long nice;
        final long system;
        final long idle;
        final long iowait;

        Data(String line) {
            Iterator<String> iter = SPLITTER_ON_SPACE.split(line).iterator();
            /*
            line:
               cpu  2255 34 2290 22625563 6290 127 456
            descr:
                user: normal processes executing in user mode
                nice: niced processes executing in user mode
                system: processes executing in kernel mode
                idle: twiddling thumbs
                iowait: waiting for I/O to complete
                irq: servicing interrupts
                softirq: servicing softirqs
            */
            iter.next();//skip 'cpu'
            user = nextLong(iter);
            nice = nextLong(iter);
            system = nextLong(iter);
            idle = nextLong(iter);
            iowait = nextLong(iter);
        }
    }

    private final File procStat;
    private volatile Data prev;
    private volatile Data curr;

    ProcStatCollector(InfoCollector ic) {
        this.procStat = new File(ic.getRootPath(), "proc/stat");
    }

    @Override
    public void refresh() throws Exception {
        try(BufferedReader r = readFile(procStat)) {
            String line = r.readLine();
            Data data = new Data(line);
            synchronized (this) {
                this.prev = this.curr;
                this.curr = data;
            }
        }
    }

    @Override
    public void fill(SysInfo info) {
        Data c;
        Data p;
        synchronized (this) {
            c = this.curr;
            p = this.prev;
        }
        if(p == null || c == null) {
            return;
        }
        float usage = (c.user - p.user) + (c.nice - p.nice) + (c.system - p.system);
        float idle = (c.idle - p.idle) + (c.iowait - p.iowait);
        float cpuLoad = 100f * usage/(usage + idle);
        info.setCpuLoad(cpuLoad);
    }
}
