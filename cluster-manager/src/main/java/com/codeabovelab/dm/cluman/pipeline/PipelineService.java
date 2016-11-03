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

package com.codeabovelab.dm.cluman.pipeline;

import com.codeabovelab.dm.cluman.pipeline.arg.PipelineDeployArg;
import com.codeabovelab.dm.cluman.pipeline.arg.PipelinePromoteArg;
import com.codeabovelab.dm.cluman.pipeline.arg.PipelineSchemaArg;
import com.codeabovelab.dm.cluman.pipeline.instance.PipelineInstance;
import com.codeabovelab.dm.cluman.pipeline.schema.PipelineSchema;

import java.util.Map;

public interface PipelineService {

    String PIPELINE_NAME = "pipelineName";
    String PIPELINE_STAGE_NAME = "pipelineStageName";
    String PIPELINE_ID = "pipelineId";

    void getOrUpdatePipeline(PipelineSchemaArg pipelineSchema);

    void promote(PipelinePromoteArg pipelinePromoteArg);

    void deploy(PipelineDeployArg pipelineDeployArg);

    void deletePipeline(String pipelineName);

    void deleteInstance(String pipelineName);

    Map<String, PipelineSchema> getPipelinesMap();

    PipelineSchema getPipeline(String pipelineId);

    Map<String, PipelineInstance> getInstancesMap();

    PipelineInstance getInstance(String pipelineInstanceId);

    Map<String, PipelineInstance> getInstancesMapByPipeline(String pipelineId);
}
