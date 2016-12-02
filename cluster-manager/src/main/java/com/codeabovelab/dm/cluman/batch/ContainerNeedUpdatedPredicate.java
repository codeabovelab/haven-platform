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
import com.codeabovelab.dm.cluman.job.JobComponent;
import com.codeabovelab.dm.cluman.job.JobParam;

import static com.codeabovelab.dm.cluman.batch.LoadContainersOfImageTasklet.JP_IMAGE;

/**
 * Test that container version is not same as target version and match source version.
 */
@JobComponent
public class ContainerNeedUpdatedPredicate implements ContainerPredicate {

    @JobParam(value = JP_IMAGE, required = true)
    private ImagesForUpdate images;

    @Override
    public boolean test(ProcessedContainer processedContainer) {
        String image = processedContainer.getImage();
        ImagesForUpdate.Image img = images.findImage(processedContainer.getImage(), processedContainer.getImageId());
        if(img == null) {
            // when happens is a bug
            throw new IllegalStateException(processedContainer + " does not has an appropriate record in images.");
        }
        return img.matchFrom(image, processedContainer.getImageId()) &&
          // when allTo == true, then matchTo - return true anyway, an we need to ignore it and
          // filter container in other place
          (img.isAllTo() || !img.matchTo(image, processedContainer.getImageId()));
    }
}
