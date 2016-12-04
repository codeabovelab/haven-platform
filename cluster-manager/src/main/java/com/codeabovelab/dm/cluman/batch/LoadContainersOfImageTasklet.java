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

import com.codeabovelab.dm.cluman.model.ImageName;
import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetContainersArg;
import com.codeabovelab.dm.cluman.job.JobComponent;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.job.JobParam;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Tasklet which is load containers of specified image and return list of its ids.
 */
@JobComponent
@Slf4j
public class LoadContainersOfImageTasklet {

    private static final String PREFIX = "LoadContainersOfImage.";
    /**
     * Job parameter 'image' - pattern of image name which containers will has been loaded.
     */
    public static final String JP_IMAGE = "images";
    public static final String JP_PERCENTAGE = PREFIX + "percentage";
    private final DockerService dockerService;
    private final JobContext context;
    private ImagesForUpdate images;
    private float percentage = 1f /* all containers, .5 - half */;
    // param need for correct docker service initialisation
    @JobParam(value = BatchUtils.JP_CLUSTER, required = true)
    private String cluster;

    @Autowired
    public LoadContainersOfImageTasklet(DockerService dockerService, JobContext context) {
        this.dockerService = dockerService;
        this.context = context;
    }

    public ImagesForUpdate getImagePattern() {
        return images;
    }

    @JobParam(value = JP_IMAGE, required = true)
    public void setImagePattern(ImagesForUpdate imagePattern) {
        this.images = imagePattern;
    }

    public float getPercentage() {
        return percentage;
    }

    @JobParam(JP_PERCENTAGE)
    public void setPercentage(float percentage) {
        this.percentage = percentage;
    }

    public List<ProcessedContainer> getContainers(ContainerPredicate predicate) {
        Assert.notNull(this.images, "Need attribute: " + JP_IMAGE);
        GetContainersArg arg = new GetContainersArg(true);
        List<DockerContainer> containers = this.dockerService.getContainers(arg);
        List<ProcessedContainer> processedContainers = new ArrayList<>();
        for(DockerContainer container : containers) {
            ImagesForUpdate.Image img = images.findImage(container.getImage(), container.getImageId());
            if(img == null) {
                log.debug("Container does not match any image: {}", container.getName());
                continue;
            }
            ProcessedContainer processedContainer = convert(container);
            if(ContainerUtils.isOurContainer(processedContainer)) {
                log.debug("Our container: {}", processedContainer.getName());
                continue;
            }

            if(!predicate.test(processedContainer)) {
                continue;
            }

            processedContainers.add(processedContainer);
        }
        if(!processedContainers.isEmpty()) {
            //choose only part of containers by percentage
            int targetSize = (int)(processedContainers.size() * this.percentage + .5f);
            if(targetSize < 1) {
                targetSize = 1;
            }
            // we can reduce size of containers with some random alg., but it is necessary?
            while(processedContainers.size() > targetSize) {
                processedContainers.remove(0);
            }
        }
        logLoadedContainers(processedContainers);
        return processedContainers;
    }

    private ProcessedContainer convert(DockerContainer dc) {
        return ProcessedContainer.builder()
          .id(dc.getId())
          .name(dc.getName())
          .node(dc.getNode().getName())
          .image(dc.getImage())
          .imageId(dc.getImageId())
          .cluster(this.cluster)
          .labels(dc.getLabels())
          .build();
    }

    private void logLoadedContainers(List<ProcessedContainer> ctrs) {
        StringBuilder sb = new StringBuilder("Load containers: ");
        for(int i = 0; i < ctrs.size(); ++i) {
            ProcessedContainer container = ctrs.get(i);
            if(i > 0) {
                sb.append(", ");
            }
            sb.append(container.getId());
        }
        context.fire(sb.toString());
    }
}
