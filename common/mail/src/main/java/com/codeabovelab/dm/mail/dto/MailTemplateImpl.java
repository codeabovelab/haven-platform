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

package com.codeabovelab.dm.mail.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.util.Assert;

/**
 * Implementation of mail template.
 */
public class MailTemplateImpl implements MailTemplate {
    public static class Builder implements MailTemplate {
        private String templateEngine;
        private MailHead headSource;
        private MailPartTemplate bodySource;

        @Override
        public String getTemplateEngine() {
            return templateEngine;
        }

        public Builder templateEngine(String templateEngine) {
            setTemplateEngine(templateEngine);
            return this;
        }

        public void setTemplateEngine(String templateEngine) {
            this.templateEngine = templateEngine;
        }

        @Override
        public MailHead getHeadSource() {
            return headSource;
        }

        public Builder headSource(MailHead headSource) {
            setHeadSource(headSource);
            return this;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = MailHeadImpl.class)
        public void setHeadSource(MailHead headSource) {
            this.headSource = headSource;
        }

        @Override
        public MailPartTemplate getBodySource() {
            return bodySource;
        }

        public Builder bodySource(MailPartTemplate bodySource) {
            setBodySource(bodySource);
            return this;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = MailPartTemplateText.class)
        public void setBodySource(MailPartTemplate bodySource) {
            this.bodySource = bodySource;
        }

        public MailTemplateImpl build() {
            return new MailTemplateImpl(this);
        }
    }

    private final MailHead headSource;
    private final String templateEngine;
    private final MailPartTemplate bodySource;

    @JsonCreator
    public MailTemplateImpl(Builder builder) {
        this.templateEngine = builder.templateEngine;
        this.headSource = builder.getHeadSource();
        Assert.notNull(this.headSource, "'headerSource' is null");
        this.bodySource = builder.bodySource;
        Assert.notNull(this.bodySource, "'bodySource' is null");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getTemplateEngine() {
        return templateEngine;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = MailHeadImpl.class)
    @Override
    public MailHead getHeadSource() {
        return headSource;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = MailPartTemplateText.class)
    @Override
    public MailPartTemplate getBodySource() {
        return this.bodySource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MailTemplateImpl)) {
            return false;
        }

        MailTemplateImpl that = (MailTemplateImpl) o;

        if (headSource != null ? !headSource.equals(that.headSource) : that.headSource != null) {
            return false;
        }
        if (templateEngine != null ? !templateEngine.equals(that.templateEngine) : that.templateEngine != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = headSource != null ? headSource.hashCode() : 0;
        result = 31 * result + (templateEngine != null ? templateEngine.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MailTemplateImpl{" +
          "headSource=" + headSource +
          ", templateEngine='" + templateEngine + '\'' +
          '}';
    }
}
