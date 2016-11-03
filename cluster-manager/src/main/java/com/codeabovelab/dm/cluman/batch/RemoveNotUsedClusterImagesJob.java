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

import com.codeabovelab.dm.cluman.cluster.docker.management.argument.RemoveImageArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.RemoveImageResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.ImageItem;
import com.codeabovelab.dm.cluman.cluster.filter.FilterFactory;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.job.JobBean;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.job.JobParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetImagesArg.ALL;
import static com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode.OK;

/**
 * Clear all not used images from cluster
 */
@JobBean("job.removeClusterImages")
public class RemoveNotUsedClusterImagesJob implements Runnable {

    @JobParam(required = true)
    private String clusterName;

    @Autowired
    private JobContext context;

    @Autowired
    private RegistryRepository registryRepository;

    @Autowired
    private DockerServices dockerServices;

    @Autowired
    private FilterFactory filterFactory;

    @Override
    public void run() {
        context.fire("About to delete all not used images from: \"{0}\".", clusterName);

        List<ImageItem> images = dockerServices.getService(clusterName).getImages(ALL);

        if (CollectionUtils.isEmpty(images)) {
            context.fire("Nothing to remove, skipping");
            return;
        }
        List<RemoveImageResult> result = images.stream().map(i -> dockerServices.getService(clusterName)
                .removeImage(RemoveImageArg.builder()
                .cluster(clusterName)
                .imageId(i.getId())
                .build())).collect(Collectors.toList());

        context.fire("Were deleted: \"{0}\", next images are used: \"{1}\"",
                filter(result, r -> r.getCode() == OK),
                filter(result, r -> r.getCode() != OK));
    }

    private List<String> filter(List<RemoveImageResult> result, Predicate<RemoveImageResult> filter) {
        return result.stream().filter(filter).map(s -> s.getImage()).collect(Collectors.toList());
    }

}

