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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.codeabovelab.dm.common.utils.Resources;
import com.codeabovelab.dm.common.utils.StringUtils;
import com.codeabovelab.dm.mail.dto.MailPartTemplate;
import com.codeabovelab.dm.mail.dto.MailPartTemplateText;
import com.codeabovelab.dm.mail.dto.MailTemplate;
import com.codeabovelab.dm.mail.dto.MailTemplateImpl;
import org.apache.commons.io.IOUtils;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

/**
 * Simple implementation for {@link com.codeabovelab.dm.mail.template.MailTemplateProvider } which retrieves
 * template from class path resource.
 */
public class ResourceMailTemplateProvider implements MailTemplateProvider {


    public static class Config {
        private String prefix = "templates";
        private String suffix;
        private MimeType mimeType;

        public String getPrefix() {
            return prefix;
        }

        public Config prefix(String prefix) {
            setPrefix(prefix);
            return this;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public Config suffix(String suffix) {
            setSuffix(suffix);
            return this;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        public MimeType getMimeType() {
            return mimeType;
        }

        public Config mimeType(MimeType mimeType) {
            setMimeType(mimeType);
            return this;
        }

        public void setMimeType(MimeType mimeType) {
            this.mimeType = mimeType;
        }
    }

    private static final String PROTOCOL = "res";
    private final Set<String> protocols;
    private final String prefix;
    private final String suffix;
    private final MimeType mimeType;
    private final ObjectMapper objectMapper;
    private final Resources.Loader<InputStream, MailPartTemplate> templatePartLoader = new Resources.Loader<InputStream, MailPartTemplate>() {
        @Override
        public MailPartTemplate apply(InputStream is) throws Exception {
            return new MailPartTemplateText(mimeType, IOUtils.toString(is, StandardCharsets.UTF_8));
        }
    };

    private final Resources.Loader<InputStream, MailTemplateImpl.Builder> descriptorLoader = new Resources.Loader<InputStream, MailTemplateImpl.Builder>() {
        @Override
        public MailTemplateImpl.Builder apply(InputStream is) throws IOException {
            MailTemplateImpl.Builder builder;
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                builder = objectMapper.readValue(reader, MailTemplateImpl.Builder.class);
            }
            return builder;
        }
    };

    public ResourceMailTemplateProvider(Config config, ObjectMapper objectMapper) {
        this.prefix = config.prefix;
        this.suffix = config.suffix;
        this.mimeType = config.mimeType;
        this.objectMapper = objectMapper;
        Assert.notNull(this.objectMapper, "'objectMapper' is null");
        Assert.hasText(this.prefix, "'prefix' must contains text");
        Assert.hasText(this.suffix, "'suffix' must contains text");
        Assert.notNull(this.mimeType, "'mimeType' is null");
        this.protocols = Collections.singleton(PROTOCOL);
    }

    @Override
    public Set<String> getProtocols() {
        return protocols;
    }

    @Override
    public MailTemplate getTemplate(String uri) {
        String path = StringUtils.after(uri, ':');
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // possibly we need to escape some chars like '..' ?
        final String templateName = this.prefix + "/" + path;
        MailTemplateImpl.Builder builder = Resources.load(classLoader, templateName + ".json", descriptorLoader);
        //in future we may implement MailPartTemplate which contains reference to another resource
        MailPartTemplate bodySource = builder.getBodySource();
        if(bodySource == null) {
            MailPartTemplate templatePart = Resources.load(classLoader, templateName + suffix, templatePartLoader);
            builder.setBodySource(templatePart);
        }
        return builder.build();
    }
}
