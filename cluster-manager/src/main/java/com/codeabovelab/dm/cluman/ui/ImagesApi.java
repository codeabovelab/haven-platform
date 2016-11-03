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

import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetImagesArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.RemoveImageArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.TagImageArg;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ResultCode;
import com.codeabovelab.dm.cluman.cluster.docker.management.result.ServiceCallResult;
import com.codeabovelab.dm.cluman.cluster.docker.model.ImageItem;
import com.codeabovelab.dm.cluman.cluster.filter.Filter;
import com.codeabovelab.dm.cluman.cluster.filter.FilterFactory;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryRepository;
import com.codeabovelab.dm.cluman.cluster.registry.RegistrySearchHelper;
import com.codeabovelab.dm.cluman.cluster.registry.RegistryService;
import com.codeabovelab.dm.cluman.cluster.registry.data.ImageCatalog;
import com.codeabovelab.dm.cluman.cluster.registry.data.SearchResult;
import com.codeabovelab.dm.cluman.ds.DockerServiceRegistry;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.ui.model.UiImageData;
import com.codeabovelab.dm.cluman.ui.model.UiSearchResult;
import com.codeabovelab.dm.cluman.ui.model.UiImageCatalog;
import com.codeabovelab.dm.cluman.ui.model.UiTagCatalog;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.cache.DefineCache;
import com.google.common.base.Splitter;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@RestController
@RequestMapping(value = "/ui/api/images", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ImagesApi {

    public static final Splitter SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();
    private final DockerServiceRegistry dockerServices;
    private final DiscoveryStorage discoveryStorage;
    private final RegistryRepository registryRepository;
    private final FilterFactory filterFactory;

    @RequestMapping(value = "/clusters/{cluster}/list", method = RequestMethod.GET)
    public List<ImageItem> getImages(@PathVariable("cluster") String cluster) {
        //TODO check usage of this method in CLI and if it not used - remove
        List<ImageItem> images = dockerServices.getService(cluster).getImages(GetImagesArg.ALL);
        return images;
    }

    @ApiOperation("search by image substring, if you specify repository then you can use expression like '*word*' ")
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public UiSearchResult search(@RequestParam(value = "registry", required = false) String registryParam,
                                 @RequestParam(value = "query", required = false) String query,
                                 @RequestParam(value = "page") int page,
                                 @RequestParam(value = "size") int size) {
        SearchResult result;
        //possibly we must place below code into 'registryRepository'
        if (!StringUtils.hasText(registryParam)) {
            // we may get registry name from query
            try {
                registryParam = ContainerUtils.getRegistryName(query);
                // registry may be a mask
                if (registryParam != null && registryParam.contains("*")) {
                    registryParam = "";
                }
            } catch (Exception e) {
                //nothing
            }
        }
        if (StringUtils.hasText(registryParam)) {
            List<String> registries = SPLITTER.splitToList(registryParam);
            RegistrySearchHelper rsh = new RegistrySearchHelper(query, page, size);
            for(String registry: registries) {
                RegistryService service = registryRepository.getByName(registry);
                if(service != null) {
                    rsh.search(service);
                }
            }
            result = rsh.collect();
        } else {
            result = registryRepository.search(query, page, size);
        }
        if (result == null) {
            return new UiSearchResult();
        }
        return UiSearchResult.from(result);
    }

    @RequestMapping(value = "/image", method = RequestMethod.GET)
    public ImageDescriptor getImage(@RequestParam("fullImageName") String fullImageName) {
        final boolean isId = ContainerUtils.isImageId(fullImageName);
        ImageDescriptor image;
        if(isId) { // id usually produced by clusters, therefore we can find it at clusters
            DockerService docker = this.discoveryStorage.getCluster(DiscoveryStorage.GROUP_ID_ALL).getDocker();
            //  not that it simply iterate over all nodes until image is appeared
            image = docker.getImage(fullImageName);
        } else {
            String name = ContainerUtils.getImageName(fullImageName);
            String registry = ContainerUtils.getRegistryName(fullImageName);
            String tag = ContainerUtils.getImageVersion(fullImageName);
            image = registryRepository.getImage(name, tag, registry);
        }
        ExtendedAssert.notFound(image, "Can not find image: " + fullImageName);
        return image;
    }

    // we must use job instead of it
    @Deprecated
    @ApiOperation("Can be passed image w/o tag then will be deleted all tags, returns list of deleted tags")
    @RequestMapping(value = "/", method = RequestMethod.DELETE)
    public List<String> removeImageFromRegistry(@RequestParam("fullImageName") String fullImageName,
                                                @RequestParam(value = "filter", required = false) String filter) {

        String name = ContainerUtils.getImageName(fullImageName);
        String registry = ContainerUtils.getRegistryName(fullImageName);
        String tag = ContainerUtils.getImageVersion(fullImageName);
        if (StringUtils.hasText(tag)) {
            ImageDescriptor image = registryRepository.getImage(name, tag, registry);
            Assert.notNull("can't find image " + name + "/" + tag);
            registryRepository.deleteTag(name, image.getId(), registry);
            return Collections.singletonList(tag);
        } else {
            return removeImagesFromRegistry(registry, name, filter);
        }
    }

    private List<String> removeImagesFromRegistry(String registry,
                                                  String name,
                                                  String filter) {
        List<String> tags = registryRepository.getTags(name, registry, getFilter(filter));
        return tags.stream().map(tag -> {
            try {
                registryRepository.deleteTag(name, tag, registry);
                return tag;
            } catch (Exception e) {
                return null;
            }
        }).filter(s -> s != null).collect(Collectors.toList());
    }

    @RequestMapping(value = "/{registry}/{name}/digest/{reference}", method = RequestMethod.DELETE)
    public void removeImageFromRegistryByReference(@PathVariable("registry") String registry,
                                                   @PathVariable("name") String name,
                                                   @PathVariable("reference") String reference) {
        registryRepository.deleteTag(name, reference, registry);
    }

    // we must use job instead of it
    @Deprecated
    @RequestMapping(value = "/clusters/{cluster}/all", method = RequestMethod.DELETE)
    @ApiResponse(message = "Returns list of deleted images", code = 200)
    public List<String> removeAllImages(@PathVariable("cluster") String cluster) {
        List<ImageItem> images = dockerServices.getService(cluster).getImages(GetImagesArg.ALL);

        List<String> collect = images.stream().map(i -> dockerServices.getService(cluster).removeImage(RemoveImageArg.builder()
                .cluster(cluster)
                .imageId(i.getId())
                .build())).filter(r -> r.getCode() != ResultCode.ERROR).map(s -> s.getImage()).collect(Collectors.toList());
        return collect;
    }

    /**
     * Tag an image into a repository
     *
     * @param repository The repository to tag in
     * @param force      (not documented)
     */
    @RequestMapping(value = "/clusters/{cluster}/tag", method = RequestMethod.PUT)
    public ResponseEntity<?> createTag(@PathVariable("cluster") String cluster,
                                       @RequestParam(value = "imageName") String imageName,
                                       @RequestParam(value = "currentTag", defaultValue = "latest") String currentTag,
                                       @RequestParam(value = "newTag") String newTag,
                                       @RequestParam(value = "repository") String repository,
                                       @RequestParam(value = "force", required = false, defaultValue = "false") Boolean force) {
        TagImageArg tagImageArg = TagImageArg.builder()
                .force(force)
                .newTag(newTag)
                .currentTag(currentTag)
                .cluster(cluster)
                .imageName(imageName)
                .repository(repository).build();
        ServiceCallResult res = dockerServices.getService(cluster).createTag(tagImageArg);

        return UiUtils.createResponse(res);
    }

    @ApiOperation("get tags, filter expression is SpEL cluster image filter")
    @RequestMapping(value = "/tags", method = GET)
    @Cacheable("TagsList")
    @DefineCache(expireAfterWrite = 60_000)
    public List<String> listTags(@RequestParam("imageName") String imageName,
                                 @RequestParam(value = "filter", required = false) String filter) {
        Filter imageFilter = getFilter(filter);
        String name = ContainerUtils.getImageName(imageName);
        String registry = ContainerUtils.getRegistryName(imageName);
        List<String> tags = registryRepository.getTags(name, registry, imageFilter);
        return tags;
    }

    private Filter getFilter(String filter) {
        if (filter == null) {
            return Filter.any();
        }
        return this.filterFactory.createFilter(filter);
    }

    @ApiOperation("get tags catalog (contains additional information), filter expression is SpEL cluster image filter")
    @RequestMapping(value = "/tags-detailed", method = GET)
    @Cacheable("UiImageCatalog")
    @DefineCache(expireAfterWrite = 60_000)
    public List<UiTagCatalog> listTagsDetailed(@RequestParam("imageName") String imageName,
                                               @RequestParam(value = "filter", required = false) String filter) {

        Filter imageFilter = getFilter(filter);
        String name = ContainerUtils.getImageName(imageName);
        String registry = ContainerUtils.getRegistryName(imageName);

        List<String> tags = registryRepository.getTags(name, registry, imageFilter);
        return tags.stream().map(t -> {
            try {
                ImageDescriptor image = registryRepository.getImage(name, t, registry);
                return new UiTagCatalog(registry, name, null, t, image != null ? image.getId() : null,
                        image != null ? image.getCreated() : null,
                        image != null ? image.getContainerConfig().getLabels() : null);
            } catch (Exception e) {
                log.error("can't download image {} / {} : {}, cause: {}", registry, name, t, e.getMessage());
                return null;
            }

        }).filter(f -> f != null).collect(Collectors.toList());

    }

    @ApiOperation("get images catalogs, filter expression is SpEL cluster image filter")
    @RequestMapping(value = "/", method = GET)
    @Cacheable("UiImageCatalog")
    @DefineCache(expireAfterWrite = 60_000)
    public List<UiImageCatalog> listImageCatalogs(@RequestParam(value = "filter", required = false) String filterStr) {
        final Filter filter = getFilter(filterStr);
        Map<String, UiImageCatalog> catalogs = getDownloadedImages(filter);
        Collection<String> registries = registryRepository.getAvailableRegistries();
        ImageObject io = new ImageObject();

        for (String registry : registries) {
            RegistryService registryService = registryRepository.getRegistry(registry);
            if (!registryService.getConfig().isDisabled()) {
                String registryName = registryService.getConfig().getName();
                ImageCatalog ic = registryService.getCatalog();
                if (ic != null) {

                    for (String name : ic.getImages()) {
                        io.setName(name);
                        io.setRegistry(registryName);
                        String fullName = StringUtils.isEmpty(registryName) ? name : registryName + "/" + name;
                        io.setFullName(fullName);
                        if (!filter.test(io)) {
                            continue;
                        }
                        //we simply create uic if it absent
                        UiImageCatalog uic = catalogs.computeIfAbsent(fullName, UiImageCatalog::new);
                    }
                }
            }
        }
        List<UiImageCatalog> list = new ArrayList<>(catalogs.values());
        Collections.sort(list);
        return list;
    }

    private Map<String, UiImageCatalog> getDownloadedImages(Filter filter) {
        //we can use result of this it for evaluate used space and deleting images, so need to se all images
        List<NodesGroup> nodesGroups = discoveryStorage.getClusters();
        Map<String, UiImageCatalog> catalogs = new TreeMap<>();
        for (NodesGroup nodesGroup : nodesGroups) {
              // we gather images from real clusters and orphans nodes
            String groupName = nodesGroup.getName();
            if (!nodesGroup.getFeatures().contains(NodesGroup.Feature.SWARM) &&
                !DiscoveryStorage.GROUP_ID_ORPHANS.equals(groupName)) {
                continue;
            }

            try {
                processGroup(filter, catalogs, nodesGroup);
            } catch (Exception e) {
                log.error("Error while process images of \"{}\"", groupName, e);
            }
        }
        return catalogs;
    }

    private void processGroup(Filter filter, Map<String, UiImageCatalog> catalogs, NodesGroup nodesGroup) {
        ImageObject io = new ImageObject();
        GetImagesArg getImagesArg = GetImagesArg.ALL;
        List<ImageItem> images = nodesGroup.getDocker().getImages(getImagesArg);
        final String clusterName = nodesGroup.getName();
        io.setCluster(clusterName);
        //note that in some cases not all nodes of cluster have same images set, but we ignore it at this time
        final List<String> nodes = nodesGroup.getNodes().stream().map(Node::getName).collect(Collectors.toList());
        io.setNodes(nodes);
        for (ImageItem image : images) {
            for (String tag : image.getRepoTags()) {
                String imageName = ContainerUtils.getRegistryAndImageName(tag);
                if(imageName.contains(ImageName.NONE)) {
                    imageName = image.getId();
                } else {
                    io.setFullName(imageName);
                }
                if (!filter.test(io)) {
                    continue;
                }
                UiImageCatalog uic = catalogs.computeIfAbsent(imageName, UiImageCatalog::new);
                if(!DiscoveryStorage.GROUP_ID_ORPHANS.equals(clusterName)) {
                    // we set name of real clusters only
                    uic.getClusters().add(clusterName);
                }
                String version = ContainerUtils.getImageVersion(tag);
                UiImageData imgdt = uic.getOrAddId(image.getId());
                imgdt.setCreated(image.getCreated());
                imgdt.setSize(image.getSize());
                if(!ImageName.NONE.equals(version)) {
                    imgdt.getTags().add(version);
                }
                imgdt.getNodes().addAll(nodes);
            }
        }
    }

    @Data
    private static class ImageObject {
        private String name;
        private String cluster;
        private Set<String> nodes = new TreeSet<>();
        private String fullName;
        private String registry;

        void setNodes(Collection<String> nodes) {
            this.nodes.clear();
            if (nodes != null) {
                this.nodes.addAll(nodes);
            }
        }

        void setFullName(String name) {
            setName(ContainerUtils.getImageName(name));
            setRegistry(ContainerUtils.getRegistryName(name));
        }

        @Override
        public String toString() {
            return fullName;
        }

        public void clear() {
            name = null;
            cluster = null;
            nodes.clear();
            fullName = null;
            registry = null;
        }
    }
}
