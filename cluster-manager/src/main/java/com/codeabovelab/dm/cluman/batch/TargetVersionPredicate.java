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

/**
 * Test that container version is not same as target version.
 */
@JobComponent
public class TargetVersionPredicate implements ContainerPredicate {

    @JobParam(value = BatchUtils.JP_IMAGE_TARGET_VERSION, required = true)
    private String targetVersion;

    @Override
    public boolean test(ProcessedContainer processedContainer) {
        String image = processedContainer.getImage();
        if(ContainerUtils.isImageId(image)) {
            // TODO strictly speaking we cannot assert that it false, but we not know imageId of target version
            return false;
        }
        return !targetVersion.equals(ContainerUtils.getImageVersion(image));
    }
}
