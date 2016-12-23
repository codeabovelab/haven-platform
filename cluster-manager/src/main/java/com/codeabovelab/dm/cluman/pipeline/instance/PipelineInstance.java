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

import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.google.common.base.MoreObjects;
import lombok.Data;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class PipelineInstance {

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

    public void setId(String id) {
        this.id = id;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    public Map<String, PipelineInstanceHistory> getHistories() {
        return Collections.unmodifiableMap(histories);
    }

    public void setHistories(Map<String, PipelineInstanceHistory> histories) {
        this.histories.clear();
        this.histories.putAll(histories);
    }

    public void addHistory(PipelineInstanceHistory history) {
        histories.put(history.getStage(), history);
    }

    public void setArgs(Map<String, String> args) {
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
        this.registry = registry;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
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
