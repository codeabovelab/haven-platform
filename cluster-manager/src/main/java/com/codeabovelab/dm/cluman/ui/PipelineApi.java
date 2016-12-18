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

import com.codeabovelab.dm.cluman.cluster.docker.management.argument.CreateContainerArg;
import com.codeabovelab.dm.cluman.model.ContainerSource;
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
@RequestMapping(value = "/ui/api/pipeline", produces = APPLICATION_JSON_VALUE)
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
        PipelineSchemaArg pipelineSchema = PipelineSchemaArg.builder()
                .name(pipeline.getName())
                .filter(pipeline.getFilter())
                .recipients(pipeline.getRecipients())
                .registry(pipeline.getRegistry())
                .pipelineStages(toStagesSchema(pipeline.getPipelineStages())).build();
        return pipelineSchema;
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

    @RequestMapping(value = "promote/{pipelineInstance}", method = PUT)
    @ApiOperation("Promote pipeline")
    public void promote(@PathVariable("pipelineInstance") String pipelineInstance,
                        @RequestBody UIPipelinePromote uiPipelinePromote) {

        PipelinePromoteArg pipelinePromoteArg = PipelinePromoteArg.builder()
                .pipelineInstance(pipelineInstance)
                .comment(uiPipelinePromote.getComment())
                .action(uiPipelinePromote.getAction())
                .build();
        pipelineService.promote(pipelinePromoteArg);

    }

    @RequestMapping(value = "deploy/{pipelineStageName}", method = PUT)
    @ApiOperation("Promote pipeline")
    public void deploy(@PathVariable("pipelineInstance") String pipelineInstance,
                       @RequestBody UIPipelineDeploy uiPipelineDeploy) {

        ContainerSource container = uiPipelineDeploy.getUiContainer();
        log.info("got create request container request at cluster: {} : {}", container.getCluster(), container);
        CreateContainerArg arg = new CreateContainerArg();
        ContainerSource copy = container.clone();
        arg.setContainer(copy);
        PipelineDeployArg deployArg = PipelineDeployArg.builder()
                .pipelineInstance(pipelineInstance)
                .createContainerArg(arg)
                .stage(uiPipelineDeploy.getStage())
                .arguments(uiPipelineDeploy.getArguments())
                .build();
        pipelineService.deploy(deployArg);
    }

    @RequestMapping(value = "pipelines", method = GET)
    @ApiOperation("List of pipelines")
    Map<String, UIPipeline> pipelinesMap() {
        return convertPipelines(pipelineService.getPipelinesMap());
    }

    private Map<String, UIPipeline> convertPipelines(Map<String, PipelineSchema> pipelinesMap) {
        return pipelinesMap.values().stream()
                .map(p -> new UIPipeline(p.getName(), p.getFilter(), p.getRegistry(), p.getRecipients(), covertStages(p.getPipelineStages())))
                .collect(Collectors.toMap(a -> a.getName(), a -> a));
    }

    private List<UIPipelineStage> covertStages(List<PipelineStageSchema> pipelineStages) {
        return pipelineStages.stream()
                .map(p -> new UIPipelineStage(p.getName(), p.getClusters(), p.getActions(), p.getTagSuffix(),
                        p.getAutoDeployLatest(), p.getRecipients()))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "pipelines/{pipelineId}", method = GET)
    UIPipeline getPipeline(@PathVariable("pipelineId") String pipelineId) {
        PipelineSchema p = pipelineService.getPipeline(pipelineId);
        if (p == null) {
            throw new NotFoundException("pipelineId was not found " + pipelineId);
        }
        return new UIPipeline(p.getName(), p.getFilter(), p.getRegistry(), p.getRecipients(), covertStages(p.getPipelineStages()));

    }

    @RequestMapping(value = "pipelineInstances", method = GET)
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
                .collect(Collectors.toMap(a -> a.getId(), a -> a));
    }

    private Map<String, UIPipelineInstanceHistory> convertHistory(Map<String, PipelineInstanceHistory> histories) {

        return histories.values().stream()
                .map(h -> new UIPipelineInstanceHistory(h.getComments(), h.getStage(), h.getTag()))
                .collect(Collectors.toMap(u -> u.getStage(), u -> u));
    }

    @RequestMapping(value = "pipelineInstances/{pipelineInstance}", method = GET)
    public UIPipelineInstance getInstance(@PathVariable("pipelineInstance") String pipelineInstance) {
        PipelineInstance p = pipelineService.getInstance(pipelineInstance);
        if (p == null) {
            throw new NotFoundException("pipelineId was not found " + pipelineInstance);
        }
        return new UIPipelineInstance(p.getId(), p.getPipeline(), p.getState(), p.getName(), p.getRegistry(),
                convertHistory(p.getHistories()), p.getArgs());
    }

    @RequestMapping(value = "pipelineInstances/byPipeline/{pipelineId}", method = GET)
    @ApiOperation("List of pipelineInstances by name of pipeline")
    public Map<String, UIPipelineInstance> getInstancesByPipeline(@PathVariable("pipelineId") String pipelineId) {
        Map<String, PipelineInstance> instancesMapByPipeline = pipelineService.getInstancesMapByPipeline(pipelineId);
        return convertInstances(instancesMapByPipeline);
    }

    @RequestMapping(value = "pipelineInstances/{pipelineInstanceId}", method = DELETE)
    @ApiOperation("Delete pipelineInstances by pipelineInstanceId")
    public void deleteInstance(@PathVariable("pipelineInstanceId") String pipelineInstanceId) {
        pipelineService.deleteInstance(pipelineInstanceId);
    }

    @RequestMapping(value = "pipeline/{pipelineId}", method = DELETE)
    @ApiOperation("Delete pipelineInstances by pipelineId")
    public void deletePipeline(@PathVariable("pipelineId") String pipelineId) {
        pipelineService.deletePipeline(pipelineId);
    }

}
