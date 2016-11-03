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

package com.codeabovelab.dm.mail.template;

import com.codeabovelab.dm.common.utils.StringUtils;
import com.codeabovelab.dm.mail.dto.MailTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 */
@Primary
@Component
public class MailTemplateProviderRegistry implements MailTemplateProvider {
    private final Map<String, MailTemplateProvider> providers;

    @Autowired
    public MailTemplateProviderRegistry(List<MailTemplateProvider> providerList) {
        Map<String, MailTemplateProvider> map = new HashMap<>();
        for(MailTemplateProvider provider: providerList) {
            for(String protocol: provider.getProtocols()) {
                MailTemplateProvider old = map.put(protocol,  provider);
                if(old != null && old != provider) {
                    throw new RuntimeException("Do not support different providers on one protocol = " + protocol + " \n" +
                      "\tone provider = " + old +
                      "\tsecond provider = " + provider );
                }
            }
        }
        this.providers = Collections.unmodifiableMap(map);
    }

    @Override
    public Set<String> getProtocols() {
        return providers.keySet();
    }

    @Override
    public MailTemplate getTemplate(String uri) {
        String proto = StringUtils.before(uri, ':');
        MailTemplateProvider provider = this.providers.get(proto);
        if(provider == null) {
            throw new MailTemplateException("Can not find provider for protocol = " + proto);
        }
        return provider.getTemplate(uri);
    }
}
