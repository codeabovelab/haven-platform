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

package com.codeabovelab.dm.cluman.pipeline.instance;

import com.codeabovelab.dm.common.kv.mapping.KvMapper;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.google.common.base.MoreObjects;
import lombok.Data;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class PipelineInstance {

    private final KvMapper<PipelineInstance> mapper;

    @KvMapping
    private String id;
    @KvMapping
    private String pipeline;

    @KvMapping
    private State state = State.IN_PROGRESS;

    // for image is full name of image w/o registry
    @KvMapping
    private String name;
    @KvMapping
    private String registry;
    @KvMapping
    private Map<String, PipelineInstanceHistory> histories = new ConcurrentHashMap<>();
    @KvMapping
    private Map<String, String> args;
    @KvMapping
    private String jobId;

    public PipelineInstance(KvMapperFactory kvmf, String pipelinePrefix, String id) {
        this.mapper = kvmf.createMapper(this, pipelinePrefix + id);
        this.id = id;
    }

    public void setId(String id) {
        this.mapper.onSet("id", this.id, id);
        this.id = id;
    }

    public void setPipeline(String pipeline) {
        this.mapper.onSet("pipeline", this.pipeline, pipeline);
        this.pipeline = pipeline;
    }

    public Map<String, PipelineInstanceHistory> getHistories() {
        return Collections.unmodifiableMap(histories);
    }

    public void setHistories(Map<String, PipelineInstanceHistory> histories) {
        this.histories.clear();
        this.histories.putAll(histories);
        this.mapper.onSet("histories", this.histories, histories);
    }

    public void addHistory(PipelineInstanceHistory history) {
        histories.put(history.getStage(), history);
        this.mapper.onSet("histories", this.histories, histories);
    }

    public void setArgs(Map<String, String> args) {
        this.mapper.onSet("args", this.args, args);
        this.args = args;
    }

    public PipelineInstanceHistory getHistoryByStage(String name) {
        return histories.get(name);
    }

    public PipelineInstanceHistory getOrCreateHistoryByStage(String name) {
        PipelineInstanceHistory pipelineInstanceHistory = histories.computeIfAbsent(name, s -> new PipelineInstanceHistory(name));
        return pipelineInstanceHistory;
    }

    public void setRegistry(String registry) {
        this.mapper.onSet("registry", this.registry, registry);
        this.registry = registry;
    }

    public void setName(String name) {
        this.mapper.onSet("name", this.name, name);
        this.name = name;
    }

    public void setState(State state) {
        this.mapper.onSet("state", this.state, state);
        this.state = state;
    }

    public void setJobId(String jobId) {
        this.mapper.onSet("jobId", this.jobId, jobId);
        this.jobId = jobId;
    }

    public KvMapper<PipelineInstance> getMapper() {
        return mapper;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mapper", mapper)
                .add("id", id)
                .add("pipeline", pipeline)
                .add("state", state)
                .add("name", name)
                .add("registry", registry)
                .add("histories", histories)
                .add("args", args)
                .add("jobId", jobId)
                .omitNullValues()
                .toString();
    }

}
