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

package com.codeabovelab.dm.cluman.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Description of job and parameters, can be used for generation ui of job manager.
 */
@Data
public class JobDescription {

    @Data
    public static class Builder {
        private String type;
        private final Map<String, JobParameterDescription> parameters = new HashMap<>();

        public Builder type(String type) {
            setType(type);
            return this;
        }

        public Builder parameter(JobParameterDescription desc) {
            parameters.put(desc.getName(), desc);
            return this;
        }

        public Builder parameters(Map<String, JobParameterDescription> parameters) {
            setParameters(parameters);
            return this;
        }

        public void setParameters(Map<String, JobParameterDescription> parameters) {
            this.parameters.clear();
            if (parameters != null) {
                this.parameters.putAll(parameters);
            }
        }

        public JobDescription build() {
            return new JobDescription(this);
        }
    }

    private final String type;
    private final Map<String, JobParameterDescription> parameters;

    @JsonCreator
    public JobDescription(Builder b) {
        this.type = b.type;
        Assert.hasText(this.type, "Job type must have text");
        this.parameters = ImmutableMap.copyOf(b.parameters);
    }

    public static Builder builder() {
        return new Builder();
    }
}
