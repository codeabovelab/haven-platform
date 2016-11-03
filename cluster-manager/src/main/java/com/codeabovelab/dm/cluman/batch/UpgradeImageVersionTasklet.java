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

import com.codeabovelab.dm.cluman.ContainerUtils;
import com.codeabovelab.dm.cluman.job.JobComponent;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.job.JobParam;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Upgrade version of container
 */
@JobComponent
public class UpgradeImageVersionTasklet {

    @JobParam(BatchUtils.JP_IMAGE_TARGET_VERSION)
    private String version;

    @Autowired
    private JobContext context;

    public ProcessedContainer execute(ProcessedContainer item) {
        final String image = item.getImage();
        final String newImage = ContainerUtils.setImageVersion(image, version);
        item = item.makeNew()
          .image(newImage)
          .imageId(null) // we also need to reset image id
          .build();
        context.fire("Upgrade version of container \"{0}\" from \"{1}\" to \"{2}\"",
                item.getName(),
                ContainerUtils.getImageVersion(image),
                version);
        return item;
    }
}
