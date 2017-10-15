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
import com.codeabovelab.dm.common.utils.AddressUtils;
import com.codeabovelab.dm.common.utils.OSUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.AbstractConfigurableEmbeddedServletContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;

/**
 */
@Component
public class DataProvider {

    private final InfoCollector collector;
    private final String hostName;
    private final String address;

    @Autowired
    public DataProvider(NotifierProps config, AbstractConfigurableEmbeddedServletContainer container) {
        this.collector = new InfoCollector(config.getRootPath());
        this.address = getAddress(config.getAddress(), container);
        this.hostName = OSUtils.getHostName();
    }

    String getHostName() {
        return hostName;
    }

    NotifierData getData() {
        NotifierData data = new NotifierData();
        data.setName(hostName);
        data.setSystem(collector.getInfo());
        data.setTime(ZonedDateTime.now());
        data.setAddress(address);
        return data;
    }

    private String getAddress(String predefinedAddress, AbstractConfigurableEmbeddedServletContainer container) {
        String proto = (container.getSsl() == null)? "http://" : "https://";
        if(StringUtils.hasText(predefinedAddress)) {
            String hostPort = AddressUtils.getHostPort(predefinedAddress);
            return proto + hostPort;
        } else {
            int port = container.getPort();
            // server must have way to fix invalid host if it need
            String host = "localhost";
            return proto + host + ":" + port;
        }
    }
}
