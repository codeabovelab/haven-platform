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

import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryService;
import com.codeabovelab.dm.cluman.job.JobComponent;
import com.codeabovelab.dm.cluman.job.JobParam;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static com.codeabovelab.dm.cluman.batch.LoadContainersOfImageTasklet.JP_IMAGE;

/**
 * Test that container version is not same as target version and match source version.
 */
@JobComponent
@Slf4j
public class ContainerNeedUpdatedPredicate implements ContainerPredicate {

    @JobParam(value = JP_IMAGE, required = true)
    private ImagesForUpdate images;

    @Autowired
    private RegistryRepository registryRepository;

    @Override
    public boolean test(ProcessedContainer processedContainer) {
        if(exclude(processedContainer)) {
            return false;
        }

        String image = processedContainer.getImage();
        ImagesForUpdate.Image img = images.findImage(image, processedContainer.getImageId());
        if (img == null) {
            throw new IllegalStateException(processedContainer + " does not has an appropriate record in images.");
        }
        return img.matchFrom(image, processedContainer.getImageId()) &&
                (!img.matchTo(image, processedContainer.getImageId()) || checkImageIdsDifferent(image, processedContainer.getImageId()));
        //TODO:
        // additionally check isAll(to) as checkImageIdsDifferent(imageName + latest, processedContainer.getImageId()))



    }

    private boolean checkImageIdsDifferent(String image, String imageId) {
        // pulling latest imageInformation
        RegistryService registryByImageName = registryRepository.getRegistryByImageName(image);
        ImageDescriptor descriptor = registryByImageName.getImage(image);
        if (descriptor == null) {
//            throw new IllegalArgumentException("ImageDescriptor was not found for " + image);
            log.warn("ImageDescriptor was not found for " + image);
            return true;
        }
        log.debug("ImageDescriptor for {}: {}, container imageId: {}", image, descriptor, imageId);
        return !imageId.equals(descriptor.getId());
    }

    private boolean exclude(ProcessedContainer pc) {
        ImagesForUpdate.Exclude exclude = images.getExclude();
        Set<String> containers = exclude.getContainers();
        return containers.contains(pc.getId()) ||
          containers.contains(pc.getName()) ||
          exclude.getNodes().contains(pc.getNode());
    }
}
