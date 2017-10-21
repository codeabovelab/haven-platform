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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class MailSourceImpl implements MailSource {

    public static class Builder implements MailSource {

        private String templateUri;
        private final Map<String, Object> variables = new HashMap<>();

        @Override
        public String getTemplateUri() {
            return templateUri;
        }

        public Builder templateUri(String templateUri) {
            setTemplateUri(templateUri);
            return this;
        }

        public void setTemplateUri(String templateUri) {
            this.templateUri = templateUri;
        }

        @Override
        public Map<String, Object> getVariables() {
            return variables;
        }

        public Builder addVariable(String key, Object var) {
            this.variables.put(key, var);
            return this;
        }

        public Builder variables(Map<String, Object> variables) {
            setVariables(variables);
            return this;
        }

        public void setVariables(Map<String, Object> variables) {
            this.variables.clear();
            if(variables != null) {
                this.variables.putAll(variables);
            }
        }

        public MailSourceImpl build() {
            return new MailSourceImpl(this);
        }
    }

    private final String templateUri;
    private final Map<String, Object> variables;

    @JsonCreator
    public MailSourceImpl(Builder builder) {
        this.templateUri = builder.templateUri;
        this.variables = Collections.unmodifiableMap(new HashMap<>(builder.variables));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getTemplateUri() {
        return templateUri;
    }

    @Override
    public Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MailSourceImpl)) {
            return false;
        }

        MailSourceImpl that = (MailSourceImpl) o;

        if (templateUri != null ? !templateUri.equals(that.templateUri) : that.templateUri != null) {
            return false;
        }
        if (variables != null ? !variables.equals(that.variables) : that.variables != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = templateUri != null ? templateUri.hashCode() : 0;
        result = 31 * result + (variables != null ? variables.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BasicMailSource{" +
          "templateUri='" + templateUri + '\'' +
          ", variables=" + variables +
          '}';
    }
}
