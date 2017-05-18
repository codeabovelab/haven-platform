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

package com.codeabovelab.dm.agent.notifier;

import com.codeabovelab.dm.agent.infocol.InfoCollector;
import com.codeabovelab.dm.common.utils.AbstractAutostartup;
import com.codeabovelab.dm.common.utils.ExecutorUtils;
import com.codeabovelab.dm.common.utils.OSUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Notify cluman server about current node.
 */
@Slf4j
@EnableConfigurationProperties(NotifierProps.class)
@Component
public class Notifier extends AbstractAutostartup {
    private final ScheduledExecutorService executor;
    private final RestTemplate restTemplate;
    private final String url;
    private final InfoCollector collector;
    private final ObjectMapper objectMapper;
    private final String hostName;

    @Autowired
    public Notifier(NotifierProps config, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.collector = new InfoCollector(config.getRootPath());
        this.hostName = OSUtils.getHostName();
        this.url = calculateUrl(config);

        this.executor = ExecutorUtils.singleThreadScheduledExecutor(this.getClass());
        ScheduledFuture<?> future = this.executor.scheduleWithFixedDelay(this::send, 60L, 60L, TimeUnit.SECONDS);
        addToClose(() -> future.cancel(true));
        addToClose(this.executor::shutdownNow);

    }

    private String calculateUrl(NotifierProps config) {
        return config.getServer() + "/discovery/nodes/" + hostName;
    }

    private void send() {
        try {
            NotifierData data = getData();
            ResponseEntity<String> resp = restTemplate.postForEntity(url, data, String.class);
            if(log.isDebugEnabled()) {
                log.debug("Send data {} to {}, with result: {}", objectMapper.writeValueAsString(data), url, resp.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Can not send to {}", url, e);
        }
    }

    private NotifierData getData() {
        NotifierData data = new NotifierData();
        data.setName(hostName);
        data.setSystem(collector.getInfo());
        data.setTime(LocalDateTime.now());
        return data;
    }
}
