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

package com.codeabovelab.dm.agent.boot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 */
@Component
@Slf4j
public class SslServletContainerCustomizer implements EmbeddedServletContainerCustomizer {
    @Value("${dm.agent.server.names:}")
    private List<String> preconfiguredNames;
    @Value("${dm.agent.server.resolve}")
    private boolean resolve;
    @Value("${dm.agent.server.keystore:${dm.data.location}/keysore.jks}")
    private String keystore;


    @Override
    public void customize(ConfigurableEmbeddedServletContainer container) {
        Cert cert = makeCert();
        Ssl ssl = new Ssl();
        ssl.setEnabled(true);
//        server.ssl.key-store=classpath:keystore.jks
//        server.ssl.key-store-password=secret
//        server.ssl.key-password=another-secret
        ssl.setKeyStore(cert.getKeystore().getAbsolutePath());
        ssl.setKeyStorePassword(cert.getKeystorePassword());
        ssl.setKeyPassword(cert.getKeyPassword());
        container.setSsl(ssl);
    }

    private Cert makeCert() {
        Set<String> names = new HashSet<>();
        if(this.preconfiguredNames != null) {
            for(String name: this.preconfiguredNames) {
                if(StringUtils.hasText(name)) {
                    names.add(name);
                }
            }
        }
        CertificateGenerator.gatherNames(names, this.resolve);
        try {
            File ksf = new File(keystore);
            ksf.getParentFile().mkdirs();
            return CertificateGenerator.constructCert(ksf, names);
        } catch (Exception e) {
            log.error("Can not generate cert.", e);
            return null;
        }
    }
}
