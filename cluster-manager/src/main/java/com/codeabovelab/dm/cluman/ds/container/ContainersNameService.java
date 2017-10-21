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

package com.codeabovelab.dm.cluman.ds.container;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.CalcNameArg;
import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.codeabovelab.dm.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Service which do calculation of new container name
 */
@Slf4j
@Component
public class ContainersNameService {

    private static final long TIMEOUT_NAMES = TimeUnit.MINUTES.toMillis(1L);

    private final Function<DockerService, Collection<String>> containerNames;
    //we store names instead last name, it allow us to use middle index instead of last
    private final ConcurrentMap<String, Long> recentNames = new ConcurrentHashMap<>();


    @Autowired
    public ContainersNameService(Function<DockerService, Collection<String>> containerNames) {
        this.containerNames = containerNames;
    }

    /**
     * Calculates unique name by pattern [image-name w/o any suffixes]-[number].
     * @param calcNameArg - all needed data
     */
    public String calculateName(CalcNameArg calcNameArg) {

        String name = internalProcess(calcNameArg);
        if (calcNameArg.isAllocate()) { storeRecentName(name); }
        log.info("name of container: {}", name);
        return name;
    }

    private String internalProcess(CalcNameArg calcNameArg) {
        if (org.springframework.util.StringUtils.hasText(calcNameArg.getContainerName())) {
            return calcNameArg.getContainerName();
        }
        String applicationName = ContainerUtils.getApplicationName(calcNameArg.getImageName()).toLowerCase();

        log.info("applicationName {}", applicationName);

        int last = getMaxNumber(applicationName, calcNameArg.getDockerService());
        if (last == -1) {
            return applicationName;
        }
        return applicationName + "-" + (last + 1);
    }

    private int getMaxNumber(String applicationName, DockerService dockerService) {
        int last = -1;
        Set<String> names = new HashSet<>();
        names.addAll(containerNames.apply(dockerService));
        names.addAll(getRecentNames());
        for (String name: names) {
            if (!(name.startsWith(applicationName))) {
                continue;
            }
            try {
                String s = StringUtils.afterLast(name, '-');
                int i = Integer.parseInt(s);
                if(i > last) {
                    last = i;
                }
            } catch(Exception e) {
                //it usual if last is word or string does not contains '-'
                if(last < 0) {
                    last = 0;
                }
            }
        }
        return last;
    }


    private void storeRecentName(String name) {
        this.recentNames.put(name, System.currentTimeMillis());
    }

    private Collection<String> getRecentNames() {
        List<String> names = new ArrayList<>();
        final long endTime = System.currentTimeMillis() - TIMEOUT_NAMES;
        for(Map.Entry<String, Long> e: recentNames.entrySet()) {
            if(e.getValue() < endTime) {
                recentNames.remove(e.getKey());
            } else {
                names.add(e.getKey());
            }
        }
        return names;
    }
}
