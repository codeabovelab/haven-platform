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

package com.codeabovelab.dm.cluman.pipeline.schema;

import com.codeabovelab.dm.common.kv.mapping.KvMapper;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.google.common.base.MoreObjects;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 */
@EqualsAndHashCode
public class PipelineSchema {

    private final KvMapper<PipelineSchema> mapper;
    @KvMapping
    private String name;
    @KvMapping
    private String filter;
    @KvMapping
    private String registry;
    @KvMapping
    private List<PipelineStageSchema> pipelineStages;
    @KvMapping
    private List<String> recipients;

    public PipelineSchema(KvMapperFactory kmf, String prefix, String name) {
        this.mapper = kmf.createMapper(this, prefix + name);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.mapper.onSet("name", this.name, name);
        this.name = name;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.mapper.onSet("filter", this.filter, filter);
        this.filter = filter;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.mapper.onSet("registry", this.registry, registry);
        this.registry = registry;
    }

    public List<PipelineStageSchema> getPipelineStages() {
        return pipelineStages;
    }

    public void setPipelineStages(List<PipelineStageSchema> pipelineStages) {
        this.mapper.onSet("pipelineStages", this.pipelineStages, pipelineStages);
        this.pipelineStages = pipelineStages;
    }

    public PipelineStageSchema getPipelineStages(String name) {
        for (PipelineStageSchema pipelineStageSchema : getPipelineStages()) {
            if (pipelineStageSchema.getName().equals(name)) {
                return pipelineStageSchema;
            }
        }
        return null;

    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.mapper.onSet("recipients", this.recipients, recipients);
        this.recipients = recipients;
    }

    public KvMapper<PipelineSchema> getMapper() {
        return mapper;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mapper", mapper)
                .add("name", name)
                .add("filter", filter)
                .add("registry", registry)
                .add("pipelineStages", pipelineStages)
                .add("recipients", recipients)
                .omitNullValues()
                .toString();
    }
}
