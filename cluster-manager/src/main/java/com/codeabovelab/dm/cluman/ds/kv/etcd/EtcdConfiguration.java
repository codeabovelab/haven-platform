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

package com.codeabovelab.dm.cluman.ds.kv.etcd;

import com.codeabovelab.dm.cluman.ds.swarm.SwarmDiscoveryUrlFunction;
import lombok.extern.slf4j.Slf4j;
import mousio.etcd4j.EtcdClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class EtcdConfiguration {


    //TODO: add auth
    @Value("${dm.kv.etcd.urls}")
    private String[] etcdUrls;

    @Value("${dm.kv.prefix:/cluman}")
    private String prefix;

    @Bean
    public EtcdClientWrapper client() {
        List<URI> uris = new ArrayList<>();
        for (String etcdUrl : etcdUrls) {
            uris.add(URI.create(etcdUrl));
        }
        log.info("About to connect to etcd: {}", (Object)etcdUrls);
        EtcdClient etcd = new EtcdClient(uris.toArray(new URI[uris.size()]));
        return new EtcdClientWrapper(etcd, prefix.trim());
    }

    @Bean
    SwarmDiscoveryUrlFunction swarmDiscoveryUrlFunction(EtcdConfiguration etcdConfiguration) {
        return new SwarmDiscoveryUrlFunction.Etcd(Arrays.stream(etcdUrls)
          .map((s) -> s.substring(s.lastIndexOf('/') + 1))
          .collect(Collectors.toList()));
    }
}
