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

package com.codeabovelab.dm.cluman.batch;

import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.codeabovelab.dm.cluman.cluster.filter.Filter;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryService;
import com.codeabovelab.dm.cluman.cluster.registry.data.Tags;
import com.codeabovelab.dm.cluman.job.JobBean;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.job.JobParam;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.codeabovelab.dm.common.utils.VersionComparator;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.codeabovelab.dm.cluman.batch.BatchUtils.JP_CLUSTER;
import static com.codeabovelab.dm.cluman.batch.LoadContainersOfImageTasklet.JP_IMAGE;

/**
 * Update matched containers to appropriate image of latest version or start a new one
 */
@JobBean(value = "job.updateToTagOrCreate", repeatable = true)
@Slf4j
@ToString
public class UpdateToTagOrCreateScheduledJob implements Runnable {

    private final ConcurrentMap<String, ImageInfo> versions = new ConcurrentHashMap<>();

    @Autowired
    private StopThenStartEachStrategy strategy;

    @Autowired
    private RegistryRepository registryRepository;

    @JobParam(BatchUtils.FILTER)
    private Filter filter;

    @Autowired
    private LoadContainersOfImageTasklet loader;

    @Autowired
    private CreateContainerTasklet containerCreator;

    @JobParam(value = JP_IMAGE, required = true)
    private String imagePattern;

    @JobParam(value = JP_CLUSTER, required = true)
    private String cluster;

    @Autowired
    private JobContext context;

    private final VersionComparator comparator = VersionComparator.builder()
            .addLatest("latest")
            .build();

    //TODO: stop after N errors!
    @Override
    public void run() {
        synchronized (versions) {
            log.debug("started job {}", this);
            log.debug("checking image for update {}", imagePattern);
            List<ProcessedContainer> containers = loader.getContainers(this::filterInner);
            if (CollectionUtils.isEmpty(containers)) {
                log.info("Nothing to update, scheduling new container");
                ImageInfo latest = getLatest(imagePattern);
                String image = ContainerUtils.setImageVersion(imagePattern, latest.getTag());
                log.info("About to create container {}", image);
                ProcessedContainer build = ProcessedContainer.builder().image(image).cluster(cluster).src(new ContainerSource()).build();
                containerCreator.execute(build);
                return;
            } else {
                strategy.run(this::filter, this::update);
            }
        }
    }

    private boolean filter(ProcessedContainer processedContainer) {

        if (!filterInner(processedContainer)) {
            return false;
        }
        String image = processedContainer.getImage();
        String version = ContainerUtils.getImageVersion(image);
        ImageInfo latest = getLatest(image);
        if (latest == null) {
            context.fire("Latest image of \"{0}\" is null, skip.", image);
            return false;
        }
        // we process only containers which have non equal version
        if (version != null && !version.equals(latest.getTag())) {
            return true;
        }
        String imageId = processedContainer.getImageId();
        Assert.notNull(imageId, "processedContainer.imageId is null");
        // TODO compare IDs
        return !imageId.equals(latest.getId());
    }

    private boolean filterInner(ProcessedContainer processedContainer) {

        if (filter == null) {
            return true;
        }
        boolean test = filter.test(processedContainer);
        if (!test) {
            log.info("container rejected by filter: {}, container: {}", filter, processedContainer);
        }
        return test;
    }


    private ProcessedContainer update(ProcessedContainer processedContainer) {
        // we update any container, because non updatable must be skipped by filter
        ImageInfo latest = getLatest(processedContainer.getImage());
        String image = processedContainer.getImage();
        String newImage = ContainerUtils.setImageVersion(image, latest.getTag());
        return processedContainer.makeNew().id(null).image(newImage).build();
    }

    private ImageInfo getLatest(String img) {
        return versions.computeIfAbsent(ContainerUtils.getRegistryAndImageName(img), (i) -> {
            RegistryService registry = registryRepository.getRegistry(ContainerUtils.getRegistryName(i));
            String imageName = ContainerUtils.getImageName(i);
            log.warn("can't find tags for {}", img);
            Tags tags = registry.getTags(imageName);
            Assert.notNull(tags, "can't find tags for " + img);
            List<String> tagsList = new ArrayList<>(tags.getTags());
            tagsList.sort(comparator);
            String tag = tagsList.get(tagsList.size() - 1);
            ImageDescriptor image = registry.getImage(imageName, tag);
            if (image == null) {
                return null;
            }
            return new ImageInfo(image.getId(), tag);
        });
    }

    @Data
    private static class ImageInfo {
        private final String id;
        private final String tag;
    }
}
