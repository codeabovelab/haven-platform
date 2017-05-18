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

import com.codeabovelab.dm.common.utils.DataSize;

import java.io.BufferedReader;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class ProcMeminfoCollector implements Collector {
    private final Pattern pattern = Pattern.compile("\\w+:\\s+(\\d+)\\s+(\\w+)");
    private final File meminfo;

    public ProcMeminfoCollector(InfoCollector ic) {
        this.meminfo = new File(ic.getRootPath(), "proc/meminfo");
    }

    @Override
    public void fill(Info info) throws Exception {
        try(BufferedReader br = InfoUtils.readFile(meminfo)) {
            /*
            * cat /proc/meminfo
                MemTotal:        8177820 kB
                MemFree:         2715188 kB
                MemAvailable:    4913576 kB
            */
            long total = parse(br.readLine());
            long free = parse(br.readLine());
            // avail - is free + file cache  & etc
            long avail = parse(br.readLine());
            Info.Memory mem = new Info.Memory();
            mem.setTotal(total);
            mem.setAvailable(free);
            // used is total - avail (which include free, unloadable file cache)
            mem.setUsed(total - avail);
            info.setMemory(mem);
        }
    }

    private long parse(String line) {
        Matcher matcher = pattern.matcher(line);
        if(!matcher.matches()) {
            throw new IllegalArgumentException("Can not match: '" + line + "' with " + pattern);
        }
        long val = Long.parseLong(matcher.group(1));
        String multStr = matcher.group(2);
        long mult = DataSize.parseMultiplier(multStr);
        return val + mult;
    }
}
