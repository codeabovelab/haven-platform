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
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetImagesArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.RemoveImageArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.ImageItem;
import com.codeabovelab.dm.cluman.cluster.filter.Filter;
import com.codeabovelab.dm.cluman.cluster.filter.FilterFactory;
import com.codeabovelab.dm.cluman.cluster.registry.ImageFilterContext;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryService;
import com.codeabovelab.dm.cluman.cluster.registry.data.Tags;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryConfig;
import com.codeabovelab.dm.cluman.ds.swarm.DockerServices;
import com.codeabovelab.dm.cluman.job.JobBean;
import com.codeabovelab.dm.cluman.job.JobContext;
import com.codeabovelab.dm.cluman.job.JobParam;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 */
@JobBean("job.removeImageJob")
public class RemoveImageJob implements Runnable {

    @JobParam(required = true)
    private String fullImageName;

    @JobParam
    private String filter;

    @JobParam
    private int retainLast;

    @JobParam
    private boolean dryRun = true;

    @JobParam
    private boolean fromRegistry;

    @JobParam
    private List<String> nodes;

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
        context.fire("dryRun={0}", dryRun);
        String name;
        String tag;
        // when image identified by 'name:tag' we have problem:
        //  on different nodes this id may point to different images,
        // ofter it appeared with 'latest' tag when one node have updated image, but other - not.
        String imageId = fullImageName;
        boolean singleImage;
        if(ContainerUtils.isImageId(fullImageName)) {
            singleImage = true;
            name = null;
            tag = null;
        } else {
            name = ContainerUtils.getImageName(fullImageName);
            tag = ContainerUtils.getImageVersion(fullImageName);
            singleImage = StringUtils.hasText(tag);
        }
        RegistryService registry = null;
        if (fromRegistry) {
            String registryName = ContainerUtils.getRegistryName(fullImageName);
            registry = registryRepository.getByName(registryName);
            ExtendedAssert.notFound(registry, "Registry: " + registryName);
        }
        if (singleImage) {
            context.fire("We remove single image: \"{0}\", delete it.", fullImageName);
            doInNodes(imageId);
            doInRegistry(registry, name, tag);
        } else {
            context.fire("Tag is unspecified, delete all tags of image \"{0}\"", fullImageName);
            if(!CollectionUtils.isEmpty(nodes)) {
                for(String nodeName: nodes) {
                    DockerService service = dockerServices.getNodeService(nodeName);
                    String regImage = ContainerUtils.getRegistryAndImageName(fullImageName);
                    List<ImageItem> images = service.getImages(GetImagesArg.builder()
                            .all(true)
                            .name(regImage)
                            .build());
                    for(ImageItem image: images) {
                        doInNode(image.getId(), service);
                    }
                }
            }
            if(registry != null) {//registry may be null only when image must not be deleted from it
                Tags tags = registry.getTags(name);
                ImageFilterContext ifc = new ImageFilterContext(registry);
                Filter ff = StringUtils.hasText(filter)? filterFactory.createFilter(filter) : Filter.any();
                ifc.setName(name);
                List<String> tagList = tags.getTags();
                //we assume that registry return sorted list fo tags
                final int end = tagList.size() - retainLast;
                context.fire("Begin tags remove:{0}\n tags retain:{1} ", tagList.subList(0, end), tagList.subList(end, tagList.size()));
                for(int i = 0; i < end; ++i) {
                    String tagItem = tagList.get(i);
                    context.fire("Process \"{0}:{1}\".", name, tagItem);
                    ifc.setTag(tagItem);
                    if(!ff.test(ifc)) {
                        context.fire("Skip \"{0}:{1}\" due to filter.", name, tagItem);
                        continue;
                    }
                    doInRegistry(registry, name, tagItem);
                }
            }
        }
    }

    private void doInNodes(String imageId) {
        if(nodes == null) {
            return;
        }
        for(String nodeName: nodes) {
            DockerService service = dockerServices.getNodeService(nodeName);
            doInNode(imageId, service);
        }
    }

    private void doInNode(String image, DockerService service) {
        RemoveImageArg removeImageArg = RemoveImageArg.builder()
          .imageId(image)
          .force(true)
          .noPrune(false)
          .build();
        ServiceCallResult res;
        if(dryRun) {
            res = new ServiceCallResult().code(ResultCode.OK).message("Dry run.");
        } else {
            res = service.removeImage(removeImageArg);
        }
        context.fire("Deletion \"{0}\" from \"{1}\" node which ended with \"{2}\"", image, service.getNode(), res);
    }

    private void doInRegistry(RegistryService registry, String name, String tag) {
        if(!fromRegistry) {
            return;
        }
        RegistryConfig config = registry.getConfig();
        String registryName = config.getName();
        if(config.isReadOnly()) {
            context.fire("Registry \"{0}\" is readonly.", registryName);
            return;
        }
        try {
            if(dryRun) {
                context.fire("Dry run deletion.");
            } else {
                registry.deleteTag(name, tag);
            }
            context.fire("Image \"{0}:{1}\" from \"{2}\" has been deleted.", name, tag, registryName);
        } catch (Exception e) {
            context.fire("Can not delete image \"{0}:{1}\" from \"{2}\", due error.", name, tag, registryName, e);
        }
    }
}
