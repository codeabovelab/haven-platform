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

import com.codeabovelab.dm.common.utils.AbstractAutostartup;
import com.codeabovelab.dm.common.utils.ExecutorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.net.URI;
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
    private final URI url;
    private final ObjectMapper objectMapper;
    private final String secret;
    private final DataProvider dataProvider;

    @Autowired
    public Notifier(NotifierProps config, RestTemplate restTemplate, ObjectMapper objectMapper, DataProvider dataProvider) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.dataProvider = dataProvider;
        this.url = calculateUrl(config);
        this.secret = config.getSecret();

        this.executor = ExecutorUtils.singleThreadScheduledExecutor(this.getClass());
        if(url != null) {
            log.warn("Server url is '{}', schedule notifier.", url);
            ScheduledFuture<?> future = this.executor.scheduleWithFixedDelay(this::send, 60L, 60L, TimeUnit.SECONDS);
            addToClose(() -> future.cancel(true));
        } else {
            log.warn("Server url is null, disable notifier.");
        }

        addToClose(this.executor::shutdownNow);
    }

    private URI calculateUrl(NotifierProps config) {
        String server = config.getServer();
        if(!StringUtils.hasText(server)) {
            return null;
        }
        return URI.create(server + "/discovery/nodes/" + dataProvider.getHostName());
    }

    private void send() {
        try {
            NotifierData data = dataProvider.getData();
            HttpHeaders headers = new HttpHeaders();
            if(secret != null) {
                headers.set(NotifierData.HEADER, secret);
            }
            RequestEntity<NotifierData> req = new RequestEntity<>(data, headers, HttpMethod.POST, url);
            ResponseEntity<String> resp = restTemplate.exchange(req, String.class);
            if(log.isDebugEnabled()) {
                log.debug("Send data {} to {}, with result: {}", objectMapper.writeValueAsString(data), url, resp.getStatusCode());
            }
        } catch (Exception e) {
            if(e instanceof ResourceAccessException) {
                // we reduce stack trace of some errors
                log.error("Can not send to {}, due to error: {}", url, e.toString());
            } else {
                log.error("Can not send to {}", url, e);
            }
        }
    }

    @PreDestroy
    public void cleanUp() {
        executor.shutdownNow();
    }
}
