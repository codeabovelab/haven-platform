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

package com.codeabovelab.dm.cluman.ui;

import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.codeabovelab.dm.cluman.model.CreateContainerArg;
import com.codeabovelab.dm.cluman.model.NotFoundException;
import com.codeabovelab.dm.cluman.pipeline.PipelineService;
import com.codeabovelab.dm.cluman.pipeline.arg.PipelineDeployArg;
import com.codeabovelab.dm.cluman.pipeline.arg.PipelinePromoteArg;
import com.codeabovelab.dm.cluman.pipeline.arg.PipelineSchemaArg;
import com.codeabovelab.dm.cluman.pipeline.arg.PipelineStageSchemaArg;
import com.codeabovelab.dm.cluman.pipeline.instance.PipelineInstance;
import com.codeabovelab.dm.cluman.pipeline.instance.PipelineInstanceHistory;
import com.codeabovelab.dm.cluman.pipeline.schema.PipelineSchema;
import com.codeabovelab.dm.cluman.pipeline.schema.PipelineStageSchema;
import com.codeabovelab.dm.cluman.ui.model.*;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping(value = "/ui/api/pipelines", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class PipelineApi {

    private final PipelineService pipelineService;

    @RequestMapping(value = "/", method = POST)
    @ApiOperation("Create new pipeline")
    public void create(@RequestBody @Valid UIPipeline pipeline) {

        PipelineSchemaArg pipelineSchema = toPipelineSchema(pipeline);
        pipelineService.getOrUpdatePipeline(pipelineSchema);
    }

    private PipelineSchemaArg toPipelineSchema(UIPipeline pipeline) {
        return PipelineSchemaArg.builder()
                .name(pipeline.getName())
                .filter(pipeline.getFilter())
                .recipients(pipeline.getRecipients())
                .registry(pipeline.getRegistry())
                .pipelineStages(toStagesSchema(pipeline.getPipelineStages())).build();
    }

    private List<PipelineStageSchemaArg> toStagesSchema(List<UIPipelineStage> pipelineStages) {
        return pipelineStages.stream().map(a -> PipelineStageSchemaArg.builder()
                .name(a.getName())
                .recipients(a.getRecipients())
                .actions(a.getActions())
                .autoDeployLatest(a.getAutoDeployLatest())
                .clusters(a.getClusters())
                .tagSuffix(a.getTagSuffix()).build()
        ).collect(Collectors.toList());
    }

    @RequestMapping(value = "/promote/{pipeline}", method = PUT)
    @ApiOperation("Promote pipeline")
    public void promote(@PathVariable("pipeline") String pipelineInstance,
                        @RequestBody UIPipelinePromote uiPipelinePromote) {

        PipelinePromoteArg pipelinePromoteArg = PipelinePromoteArg.builder()
                .pipelineInstance(pipelineInstance)
                .comment(uiPipelinePromote.getComment())
                .action(uiPipelinePromote.getAction())
                .build();
        pipelineService.promote(pipelinePromoteArg);
    }

    @RequestMapping(value = "/{id}", method = DELETE)
    @ApiOperation("Delete pipelineInstances by pipelineId")
    public void deletePipeline(@PathVariable("id") String pipelineId) {
        pipelineService.deletePipeline(pipelineId);
    }

    @RequestMapping(value = "/deploy/{pipeline}", method = PUT)
    @ApiOperation("Promote pipeline")
    public void deploy(@PathVariable("pipeline") String pipelineInstance,
                       @RequestBody UIPipelineDeploy uiPipelineDeploy) {

        ContainerSource container = uiPipelineDeploy.getUiContainer();
        log.info("got create request container request at cluster: {} : {}", container.getCluster(), container);
        ContainerSource copy = container.clone();
        CreateContainerArg arg = new CreateContainerArg()
                .enrichConfigs(true).container(copy);
        PipelineDeployArg deployArg = PipelineDeployArg.builder()
                .pipelineInstance(pipelineInstance)
                .createContainerArg(arg)
                .stage(uiPipelineDeploy.getStage())
                .arguments(uiPipelineDeploy.getArguments())
                .build();
        pipelineService.deploy(deployArg);
    }

    @RequestMapping(value = "/", method = GET)
    @ApiOperation("List of pipelines")
    Map<String, UIPipeline> pipelinesMap() {
        return convertPipelines(pipelineService.getPipelinesMap());
    }

    private Map<String, UIPipeline> convertPipelines(Map<String, PipelineSchema> pipelinesMap) {
        return pipelinesMap.values().stream()
                .map(p -> new UIPipeline(p.getName(), p.getFilter(), p.getRegistry(), p.getRecipients(), covertStages(p.getPipelineStages())))
                .collect(Collectors.toMap(UIPipeline::getName, a -> a));
    }

    private List<UIPipelineStage> covertStages(List<PipelineStageSchema> pipelineStages) {
        return pipelineStages.stream()
                .map(p -> new UIPipelineStage(p.getName(), p.getClusters(), p.getActions(), p.getTagSuffix(),
                        p.getAutoDeployLatest(), p.getRecipients()))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/{id}", method = GET)
    UIPipeline getPipeline(@PathVariable("id") String pipelineId) {
        PipelineSchema p = pipelineService.getPipeline(pipelineId);
        if (p == null) {
            throw new NotFoundException("pipelineId was not found " + pipelineId);
        }
        return new UIPipeline(p.getName(), p.getFilter(), p.getRegistry(), p.getRecipients(), covertStages(p.getPipelineStages()));

    }

    @RequestMapping(value = "/instances", method = GET)
    @ApiOperation("List of pipelineInstances")
    Map<String, UIPipelineInstance> instancesMap() {
        Map<String, PipelineInstance> instancesMap = pipelineService.getInstancesMap();
        return convertInstances(instancesMap);
    }

    private Map<String, UIPipelineInstance> convertInstances(Map<String, PipelineInstance> instancesMap) {
        return instancesMap.values().stream()
                .map(p -> UIPipelineInstance.builder()
                        .id(p.getId())
                        .pipeline(p.getPipeline())
                        .state(p.getState())
                        .name(p.getName())
                        .registry(p.getRegistry())
                        .histories(convertHistory(p.getHistories()))
                        .args(p.getArgs())
                        .build())
                .collect(Collectors.toMap(UIPipelineInstance::getId, a -> a));
    }

    private Map<String, UIPipelineInstanceHistory> convertHistory(Map<String, PipelineInstanceHistory> histories) {

        return histories.values().stream()
                .map(h -> new UIPipelineInstanceHistory(h.getComments(), h.getStage(), h.getTag()))
                .collect(Collectors.toMap(UIPipelineInstanceHistory::getStage, u -> u));
    }

    @RequestMapping(value = "instances/{id}", method = GET)
    public UIPipelineInstance getInstance(@PathVariable("id") String pipelineInstance) {
        PipelineInstance p = pipelineService.getInstance(pipelineInstance);
        if (p == null) {
            throw new NotFoundException("pipelineId was not found " + pipelineInstance);
        }
        return new UIPipelineInstance(p.getId(), p.getPipeline(), p.getState(), p.getName(), p.getRegistry(),
                convertHistory(p.getHistories()), p.getArgs());
    }

    @RequestMapping(value = "instances/byPipeline/{id}", method = GET)
    @ApiOperation("List of pipelineInstances by name of pipeline")
    public Map<String, UIPipelineInstance> getInstancesByPipeline(@PathVariable("id") String pipelineId) {
        Map<String, PipelineInstance> instancesMapByPipeline = pipelineService.getInstancesMapByPipeline(pipelineId);
        return convertInstances(instancesMapByPipeline);
    }

    @RequestMapping(value = "instances/{id}", method = DELETE)
    @ApiOperation("Delete pipelineInstances by pipelineInstanceId")
    public void deleteInstance(@PathVariable("id") String pipelineInstanceId) {
        pipelineService.deleteInstance(pipelineInstanceId);
    }

}
