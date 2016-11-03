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

import com.codeabovelab.dm.cluman.ContainerUtils;
import com.codeabovelab.dm.cluman.batch.LoadContainersOfImageTasklet;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.CreateContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetContainersArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.TagImageArg;
import com.codeabovelab.dm.cluman.cluster.filter.Filter;
import com.codeabovelab.dm.cluman.cluster.filter.LabelFilter;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.ds.DockerServiceRegistry;
import com.codeabovelab.dm.cluman.ds.container.ContainerManager;
import com.codeabovelab.dm.common.kv.DeleteDirOptions;
import com.codeabovelab.dm.common.kv.KeyValueStorage;
import com.codeabovelab.dm.common.kv.KvUtils;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.cluman.job.JobInstance;
import com.codeabovelab.dm.cluman.job.JobParameters;
import com.codeabovelab.dm.cluman.job.JobsManager;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import com.codeabovelab.dm.cluman.model.Severity;
import com.codeabovelab.dm.cluman.pipeline.arg.PipelineDeployArg;
import com.codeabovelab.dm.cluman.pipeline.arg.PipelinePromoteArg;
import com.codeabovelab.dm.cluman.pipeline.arg.PipelineSchemaArg;
import com.codeabovelab.dm.cluman.pipeline.arg.PipelineStageSchemaArg;
import com.codeabovelab.dm.cluman.pipeline.instance.PipelineInstance;
import com.codeabovelab.dm.cluman.pipeline.instance.PipelineInstanceHistory;
import com.codeabovelab.dm.cluman.pipeline.instance.State;
import com.codeabovelab.dm.cluman.pipeline.schema.PipelineSchema;
import com.codeabovelab.dm.cluman.pipeline.schema.PipelineStageSchema;
import com.codeabovelab.dm.cluman.ui.model.PipeLineAction;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.security.TempAuth;
import com.codeabovelab.dm.common.utils.Throwables;
import com.codeabovelab.dm.common.utils.Uuids;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.codeabovelab.dm.cluman.batch.BatchUtils.*;

@Component
@Slf4j
public class PipelineServiceImpl implements PipelineService {

    private final LoadingCache<String, PipelineSchema> pipelineSchemas;
    private final LoadingCache<String, PipelineInstance> pipelineInstances;
    private final KvMapperFactory kvmf;
    private final MessageBus<PipelineEvent> pipelineEventBus;
    private final String pipelinePrefix;
    private final String pipelineInstancePrefix;
    private final DockerServiceRegistry dockerServiceRegistry;
    private final JobsManager jobsManager;
    private final RegistryRepository registryRepository;
    private final ContainerManager containerManager;

    private final String updateCronExpression;

    @Autowired
    public PipelineServiceImpl(KvMapperFactory kvmf,
                               DockerServiceRegistry dockerServiceRegistry,
                               RegistryRepository registryRepository,
                               ContainerManager containerManager,
                               @Qualifier(PipelineEvent.BUS) MessageBus<PipelineEvent> pipelineEventBus,
                               JobsManager jobsManager,
                               @Value("${dm.pipeline.updateCronExpression:* * * * * *}") String updateCronExpression) {
        this.kvmf = kvmf;
        this.jobsManager = jobsManager;
        this.registryRepository = registryRepository;
        this.pipelineEventBus = pipelineEventBus;
        this.dockerServiceRegistry = dockerServiceRegistry;
        this.containerManager = containerManager;
        KeyValueStorage storage = kvmf.getStorage();
        this.updateCronExpression = updateCronExpression;
        this.pipelinePrefix = storage.getDockMasterPrefix() + "/pipelines/";
        this.pipelineInstancePrefix = storage.getDockMasterPrefix() + "/pipelineInstances/";
        this.pipelineSchemas = CacheBuilder.newBuilder().build(new CacheLoader<String, PipelineSchema>() {
            @Override
            public PipelineSchema load(String name) throws Exception {
                PipelineSchema nr = makeNewPipelineSchema(name);
                return nr;
            }
        });

        pipelineInstances = CacheBuilder.newBuilder().build(new CacheLoader<String, PipelineInstance>() {
            @Override
            public PipelineInstance load(String name) throws Exception {
                PipelineInstance nr = makeNewPipelineInstance(name);
                return nr;
            }

        });
    }

    private PipelineInstance makeNewPipelineInstance(String id) {
        try {
            PipelineInstance pipelineInstance = new PipelineInstance(kvmf, pipelineInstancePrefix, id);
            pipelineInstance.getMapper().load();
            return pipelineInstance;
        } catch (Exception e) {
            log.error("Can't load pipelineSchema by key {}", pipelineInstancePrefix + id);
//            kvmf.getStorage().delete(pipelineInstancePrefix + id, WriteOptions.builder().build());
            throw Throwables.asRuntime(e);
        }
    }

    private PipelineSchema makeNewPipelineSchema(String name) {
        try {
            PipelineSchema pipelineSchema = new PipelineSchema(kvmf, pipelinePrefix, name);
            pipelineSchema.getMapper().load();
            return pipelineSchema;
        } catch (Exception e) {
            log.error("Can't load pipelineSchema by key {}", pipelinePrefix + name);
//            kvmf.getStorage().delete(pipelinePrefix + name, WriteOptions.builder().build());
            throw Throwables.asRuntime(e);
        }
    }

    @Override
    public void getOrUpdatePipeline(PipelineSchemaArg pipelineSchemaArg) {
        PipelineSchema pipelineSchema = makeNewPipelineSchema(pipelineSchemaArg.getName());
        pipelineSchema.setRecipients(pipelineSchemaArg.getRecipients());
        pipelineSchema.setRegistry(pipelineSchemaArg.getRegistry());
        pipelineSchema.setFilter(pipelineSchemaArg.getFilter());
        pipelineSchema.setPipelineStages(toStagesSchema(pipelineSchemaArg.getPipelineStages()));
        checkPipeline(pipelineSchema);
        pipelineSchema.getMapper().save();
        riseEvent(pipelineSchema, "create");
    }

    //TODO: change approach
    @Scheduled(fixedDelay = Long.MAX_VALUE, initialDelay = 60_000)
    public void init() {
        try (TempAuth ta = TempAuth.asSystem()) {
            log.debug("init pipelines");
            Map<String, PipelineSchema> pipelinesMap = getPipelinesMap();
            for (PipelineSchema pipelineSchema : pipelinesMap.values()) {
                try {
                    checkPipeline(pipelineSchema);
                } catch (Exception e) {
                    log.error("error due loading pipelineSchema " + pipelineSchema.toString(), e);
                }
            }
        }

    }

    private void riseEvent(PipelineSchema pipelineSchema, String action) {
        PipelineEvent.Builder event = PipelineEvent.builder();
        event.setAction(action);
        event.setName(pipelineSchema.getName());
        event.setSeverity(Severity.INFO);
        pipelineEventBus.accept(event.build());
    }

    private void checkPipeline(PipelineSchema pipelineSchema) {
        pipelineSchema.getPipelineStages().forEach(a -> checkPipelineStage(pipelineSchema, a));
    }

    private List<PipelineInstance> checkPipelineStage(PipelineSchema pipelineSchema, PipelineStageSchema stage) {

        log.info("checking pipeline {}", pipelineSchema);
        List<PipelineInstance> pipelineInstances = new ArrayList<>();
        List<String> clusters = stage.getClusters();
        for (String cluster : clusters) {
            DockerService service = dockerServiceRegistry.getService(cluster);
            List<DockerContainer> containers = service.getContainers(new GetContainersArg(true));
            List<PipelineInstance> collect = containers.stream()
                    .filter(a -> filterContainers(a, pipelineSchema, stage) && a.getLabels().get(PIPELINE_ID) != null)
                    .map(c -> {
                        try {
                            return createPipelineInstance(c.getLabels().get(PIPELINE_ID), c.getImage(), pipelineSchema, stage);
                        } catch (Exception e) {
                            log.error("can't create pipeline", e);
                            return null;
                        }
                    })
                    .collect(Collectors.toList());
            pipelineInstances.addAll(collect);
        }
        Map<String, PipelineInstance> instancesMapByPipeline = getInstancesMapByPipeline(pipelineSchema.getName());

        if (!instancesMapByPipeline.isEmpty()) {
            instancesMapByPipeline.values().stream().forEach(pipelineInstance -> checkCreateJobs(pipelineSchema, stage, pipelineInstance));
        } else {
            String name = pipelineSchema.getName() + ":" + pipelineSchema.getFilter();
            createPipelineInstance(name, name, pipelineSchema, stage);
        }
        return pipelineInstances;

    }


    private void addJob(PipelineSchema pipelineSchema, PipelineStageSchema stage, PipelineInstance instance) {
        if (instance.getJobId() == null || jobsManager.getJob(instance.getJobId()) == null) {
            JobParameters jobParameters = JobParameters.builder()
                    .type("job.updateToTagOrCreate")
                    .schedule(updateCronExpression)
                    .title(pipelineSchema.getName() + ":" + stage.getName() + "-" + instance.getId())
                    .parameters(createParameters(stage, pipelineSchema, instance)).build();
            JobInstance jobInstance = jobsManager.create(jobParameters);
            jobInstance.start();
            instance.setJobId(jobInstance.getInfo().getId());
            instance.getMapper().save();
            log.info("added new job {}", jobInstance);
        }
    }

    private Map<String, Object> createParameters(PipelineStageSchema stage, PipelineSchema pipelineSchema, PipelineInstance instance) {
        String image = pipelineSchema.getRegistry() + "/" + pipelineSchema.getFilter();
        Map<String, Object> map = new HashMap<>();
        map.put(JP_CLUSTER, stage.getClusters().get(0));
        map.put(LoadContainersOfImageTasklet.JP_IMAGE, image);
        String tagSuffix = stage.getTagSuffix();
        if (!StringUtils.hasText(tagSuffix)) {
            tagSuffix = "latest";
        }
        map.put(JP_IMAGE_TARGET_VERSION, tagSuffix);
        map.put(JP_ROLLBACK_ENABLE, false);
        Map<String, Object> containers = new HashMap<>();
        Map<String, Object> container = new HashMap<>();
        containers.put(image, container);
        String labels = "labels.";
        container.put(labels + PIPELINE_NAME, pipelineSchema.getName());
        container.put(labels + PIPELINE_STAGE_NAME, stage.getName());
        container.put(labels + PIPELINE_ID, instance.getId());
        map.put("container", containers);
        map.put(JP_ROLLBACK_ENABLE, false);

        //creating filter
        Map<String, String> requiredLabels = new HashMap<>(getRequiredLabels(pipelineSchema, stage));
        requiredLabels.put(PIPELINE_ID, instance.getId());
        map.put(FILTER, createLabelFilter(requiredLabels));

        //we pass random id, instead job will be cached
        map.put("id", Uuids.liteRandom());
        return map;
    }

    private Filter createLabelFilter(Map<String, String> stage) {
        LabelFilter labelFilter = new LabelFilter(stage);
        log.info("LabelFilter: {}", labelFilter);
        return labelFilter;
    }

    public PipelineInstance createPipelineInstance(String id,
                                                   String name,
                                                   PipelineSchema pipelineSchema,
                                                   PipelineStageSchema pipelineStageName) {

        Assert.notNull(id, "Pipeline id can't be null");
        PipelineInstance instance = makeNewPipelineInstance(id);
        instance.setPipeline(pipelineSchema.getName());
        instance.setName(name);
        instance.getOrCreateHistoryByStage(pipelineStageName.getName());
        log.info("created PipelineInstance {}", instance);

        checkCreateJobs(pipelineSchema, pipelineStageName, instance);

        instance.getMapper().save();
        return instance;

    }

    private void checkCreateJobs(PipelineSchema pipelineSchema, PipelineStageSchema stage, PipelineInstance instance) {
        if (Boolean.TRUE.equals(stage.getAutoDeployLatest()) && !jobsManager.getTypes().contains(stage.getName())) {
            addJob(pipelineSchema, stage, instance);
        }
    }

    @Override
    public void promote(PipelinePromoteArg pipelinePromoteArg) {
        log.info("try to promote image", pipelinePromoteArg);
        String id = pipelinePromoteArg.getPipelineInstance();
        PipeLineAction action = pipelinePromoteArg.getAction();
        PipelineInstance instance = getPipelineInstance(id);
        PipelineSchema pipelineSchema = getPipelineSchema(instance.getPipeline());
        List<PipelineStageSchema> pipelineStages = pipelineSchema.getPipelineStages();
        PipelineInstanceHistory lastHistory = instance.getOrCreateHistoryByStage(pipelinePromoteArg.getStage());
        lastHistory.addComments(pipelinePromoteArg.getComment());
        switch (action) {
            case PROMOTE:
                instance.setState(State.IN_PROGRESS);

                PipelineStageSchema nextStage = null;
                PipelineStageSchema currentStage = null;
                for (int i = 0; i < pipelineStages.size(); i++) {
                    PipelineStageSchema pipelineStage = pipelineStages.get(i);
                    if (pipelineStage.getName().equals(lastHistory.getStage())) {
                        currentStage = pipelineStage;
                        if (i < pipelineStages.size() - 1) {
                            nextStage = pipelineStages.get(i + 1);
                        }
                        break;
                    }
                }
                if (nextStage == null) {
                    instance.setState(State.FINISHED);
                    return;
                }
                PipelineInstanceHistory nextHistory = instance.getOrCreateHistoryByStage(nextStage.getName());
                String tag = createTag(currentStage, nextStage, instance, pipelineSchema);
                nextHistory.setTag(tag);
                instance.getMapper().save();
                break;
            case REJECT:
                instance.setState(State.INTERRUPTED);
                deleteTag(instance, lastHistory);
                break;
        }
        riseEvent(pipelineSchema, action.name());

    }

    private void deleteTag(PipelineInstance instance, PipelineInstanceHistory lastHistory) {
        registryRepository.deleteTag(instance.getRegistry(), instance.getName(), lastHistory.getTag());
    }

    @Override
    public void deploy(PipelineDeployArg pipelineDeployArg) {
        CreateContainerArg createContainerArg = pipelineDeployArg.getCreateContainerArg();
        String comment = pipelineDeployArg.getComment();
        String pipelineInstanceId = pipelineDeployArg.getPipelineInstance();
        PipelineInstance instance = getPipelineInstance(pipelineInstanceId);
        PipelineSchema pipelineSchema = getPipelineSchema(instance.getId());
        PipelineInstanceHistory history = instance.getOrCreateHistoryByStage(pipelineDeployArg.getStage());
        PipelineStageSchema pipelineStages = pipelineSchema.getPipelineStages(pipelineDeployArg.getStage());
        history.addComments(comment);
        Map<String, String> labels = createContainerArg.getContainer().getLabels();
        labels.putAll(getRequiredLabels(pipelineSchema, pipelineStages));
        labels.put(PIPELINE_ID, pipelineInstanceId);
        containerManager.createContainer(createContainerArg);
        instance.getMapper().save();
        riseEvent(pipelineSchema, "deploy");

    }

    @Override
    public void deletePipeline(String pipelineName) {
        Map<String, PipelineInstance> instancesMapByPipeline = getInstancesMapByPipeline(pipelineName);
        instancesMapByPipeline.values().stream().filter(a -> a != null).forEach(this::deleteInstance);

        PipelineSchema pipelineSchema = getPipelineSchema(pipelineName);
        Assert.notNull(pipelineSchema, "Can't find pipeline by name " + pipelineSchema);

        final String path = KvUtils.join(pipelinePrefix, pipelineSchema.getName());
        kvmf.getStorage().deletedir(path, DeleteDirOptions.builder().recursive(true).build());
        riseEvent(pipelineSchema, "delete");

    }

    @Override
    public void deleteInstance(String pipelineName) {

        PipelineInstance instance = getInstance(pipelineName);
        if (instance != null) {
            deleteInstance(instance);
        }
    }

    private void deleteInstance(PipelineInstance instance) {
        JobInstance job = jobsManager.getJob(instance.getJobId());
        if (job != null) {
            job.cancel();
        }

        pipelineInstances.invalidate(instance.getId());
        final String path = KvUtils.join(pipelineInstancePrefix, instance.getId());
        kvmf.getStorage().deletedir(path, DeleteDirOptions.builder().recursive(true).build());
    }


    @Override
    public Map<String, PipelineSchema> getPipelinesMap() {
        List<String> allPipelines = getAllPipelines();
        return allPipelines.stream().map(s -> makeNewPipelineSchema(s.substring(pipelinePrefix.length())))
                .collect(Collectors.toMap(s -> s.getName(), s -> s));
    }

    private List<String> getAllPipelines() {
        try {
            List<String> list = kvmf.getStorage().list(pipelinePrefix);
            return MoreObjects.firstNonNull(list, Collections.emptyList());
        } catch (Exception e) {
            log.warn(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public PipelineSchema getPipeline(String pipelineId) {
        PipelineSchema pipelineSchema = getPipelineSchema(pipelineId);
        return pipelineSchema;
    }

    @Override
    public Map<String, PipelineInstance> getInstancesMap() {
        List<String> allPipelineInstances = getAllPipelineInstances();
        return allPipelineInstances.stream().map(s -> makeNewPipelineInstance(s.substring(pipelineInstancePrefix.length())))
                .collect(Collectors.toMap(s -> s.getId(), s -> s));
    }

    private List<String> getAllPipelineInstances() {
        try {
            return kvmf.getStorage().list(pipelineInstancePrefix);
        } catch (Exception e) {
            log.warn(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public PipelineInstance getInstance(String pipelineInstanceId) {
        PipelineInstance pipelineInstance = getPipelineInstance(pipelineInstanceId);
        return pipelineInstance;
    }

    @Override
    public Map<String, PipelineInstance> getInstancesMapByPipeline(String pipelineId) {
        try {
            Map<String, PipelineInstance> instancesMap = getInstancesMap();
            return instancesMap.values().stream()
                    .filter(p -> p.getPipeline().equals(pipelineId))
                    .collect(Collectors.toMap(a -> a.getId(), a -> a));
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public PipelineInstance getPipelineInstance(String id) {
        try {
            PipelineInstance instance = pipelineInstances.get(id);
            Assert.notNull(instance, "can't find instance " + id);
            return instance;
        } catch (ExecutionException e) {
            throw Throwables.asRuntime(e);
        }
    }

    public PipelineSchema getPipelineSchema(String id) {
        try {
            PipelineSchema instance = pipelineSchemas.get(id);
            Assert.notNull(instance, "can't find instance " + id);
            return instance;
        } catch (ExecutionException e) {
            throw Throwables.asRuntime(e);
        }
    }

    protected String createTag(PipelineStageSchema stage, PipelineStageSchema nextStage,
                               PipelineInstance instance, PipelineSchema pipelineSchema) {
        PipelineInstanceHistory history = instance.getHistoryByStage(stage.getName());
        String fullImage = ContainerUtils.buildImageName(pipelineSchema.getRegistry(), pipelineSchema.getFilter(), history.getTag());
        return createTag(stage.getClusters().get(0), fullImage, nextStage.getTagSuffix());
    }

    protected String createTag(String clusterName, String fullImage, String nextTag) {
        DockerService service = dockerServiceRegistry.getService(clusterName);
        String imageVersion = ContainerUtils.getImageVersion(fullImage);
        if (imageVersion == null) {
            imageVersion = "latest";
        }
        TagImageArg tagImageArg = TagImageArg.builder()
                .imageName(ContainerUtils.getImageName(fullImage))
                .repository(ContainerUtils.getRegistryName(fullImage))
                .currentTag(imageVersion)
                .newTag(nextTag).build();
        log.info("pushing image {}", tagImageArg);
        service.createTag(tagImageArg);

        return nextTag;
    }

    private Map<String, String> getRequiredLabels(PipelineSchema pipelineSchema, PipelineStageSchema stage) {
        return ImmutableMap.of(
                PIPELINE_NAME, pipelineSchema.getName(),
                PIPELINE_STAGE_NAME, stage.getName());
    }

    private boolean filterContainers(DockerContainer dockerContainer, PipelineSchema pipelineSchema, PipelineStageSchema stage) {
        LabelFilter labelFilter = new LabelFilter(getRequiredLabels(pipelineSchema, stage));
        return labelFilter.test(dockerContainer.getLabels());
    }

    private List<PipelineStageSchema> toStagesSchema(List<PipelineStageSchemaArg> pipelineStages) {
        return pipelineStages.stream().map(a -> {
            PipelineStageSchema pps = new PipelineStageSchema();
            pps.setName(a.getName());
            pps.setRecipients(a.getRecipients());
            pps.setActions(a.getActions());
            pps.setAutoDeployLatest(a.getAutoDeployLatest());
            pps.setClusters(a.getClusters());
            pps.setTagSuffix(a.getTagSuffix());
            return pps;
        }).collect(Collectors.toList());
    }

}
