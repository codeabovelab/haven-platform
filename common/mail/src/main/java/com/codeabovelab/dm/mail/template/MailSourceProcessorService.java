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

import com.codeabovelab.dm.mail.dto.MailMessage;
import com.codeabovelab.dm.mail.dto.MailSource;
import com.codeabovelab.dm.mail.dto.MailTemplate;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service which process mail source and specified template for producing of mail messages.
 */
@Component
public class MailSourceProcessorService {

    private final MailTemplateProvider templateProvider;
    private final Map<String, MailTemplateEngine> templateEngines;

    /**
     * Do not use this ctor directly.
     * @param provider
     * @param engines
     */
    @Autowired
    public MailSourceProcessorService(MailTemplateProvider provider, List<MailTemplateEngine> engines) {
        this.templateProvider = provider;
        ImmutableMap.Builder<String, MailTemplateEngine> imb = ImmutableMap.builder();
        engines.forEach(e -> e.getProvidedEngines().forEach(name -> imb.put(name, e)));
        this.templateEngines = imb.build();
    }

    public MailMessage process(MailSource mailSource) {
        Assert.notNull(mailSource, "mailSource is null");
        String templateUri = mailSource.getTemplateUri();
        Assert.notNull(templateUri, "mailSource.templateUri is null");
        final MailTemplate mailTemplate = getMailTemplate(templateUri);
        return getTemplateEngine(mailTemplate).create(mailTemplate, mailSource);
    }

    private MailTemplate getMailTemplate(String templateUri) {
        final MailTemplate mailTemplate = this.templateProvider.getTemplate(templateUri);
        if(mailTemplate == null) {
            throw new MailTemplateException("can not find mail template: " + templateUri);
        }
        return mailTemplate;
    }

    public List<MailMessage> process(MailTemplate template, List<MailSource> sources) {
        Assert.notNull(template, "template is null");
        Assert.notNull(sources, "source is null");
        MailTemplateEngine engine = getTemplateEngine(template);
        List<MailMessage> messages = new ArrayList<>();
        for(MailSource source: sources) {
            MailMessage message = engine.create(template, source);
            messages.add(message);
        }
        return messages;
    }

    private MailTemplateEngine getTemplateEngine(MailTemplate template) {
        MailTemplateEngine engine;
        String engineName = template.getTemplateEngine();
        engine = templateEngines.get(engineName);
        if(engine == null) {
            throw new MailTemplateException("Can not find engine: " + engineName +
              ", known engines are: " + templateEngines.keySet());
        }
        return engine;
    }
}
