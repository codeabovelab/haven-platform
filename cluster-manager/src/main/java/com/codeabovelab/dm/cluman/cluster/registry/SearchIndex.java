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

package com.codeabovelab.dm.cluman.cluster.registry;

import com.codeabovelab.dm.cluman.ContainerUtils;
import com.codeabovelab.dm.cluman.cluster.registry.data.ImageCatalog;
import com.codeabovelab.dm.cluman.cluster.registry.data.SearchResult;
import com.codeabovelab.dm.cluman.cluster.registry.data.Tags;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import com.codeabovelab.dm.cluman.model.StandardActions;
import com.codeabovelab.dm.common.utils.SingleValueCache;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PatternMatchUtils;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 */
@Slf4j
class SearchIndex implements SupportSearch, AutoCloseable {
    private static final long TIMEOUT = TimeUnit.MINUTES.toMillis(2L);
    public static final String LABEL_DESCRIPTION = "description";
    private final RegistryService service;
    private final SingleValueCache<Map<String, ImageInfo>> cache;
    private final String registryName;
    private final ScheduledExecutorService ses;
    private ScheduledFuture<?> future;

    public SearchIndex(RegistryService service, ScheduledExecutorService scheduledExecutorService) {
        this.service = service;
        this.registryName = this.service.getConfig().getName();
        this.ses = scheduledExecutorService;
        this.cache = SingleValueCache.builder(this::load).timeAfterWrite(TimeUnit.MILLISECONDS, getTimeout()).build();
    }

    private Map<String, ImageInfo> load() {
        long begin = System.currentTimeMillis();
        //sometime we may found duplicates
        String regId = registryName + "@" + Objects.hashCode(service);
        log.info("Begin load index of {} ", regId);
        Map<String, ImageInfo> old = this.cache.getOldValue();
        Map<String, ImageInfo> images = new HashMap<>();
        ImageCatalog catalog = this.service.getCatalog();
        if(catalog == null) {
            log.info("Catalog of {} is null, see above log for details.", regId);
        } else {
            for(String image: catalog.getImages()) {
                String fullName = ContainerUtils.buildImageName(registryName, image, null);
                ImageDescriptor descriptor = loadDescriptor(regId, image);
                ImageInfo ii = new ImageInfo(fullName, descriptor);
                images.put(fullName, ii);
            }
        }
        float seconds = (System.currentTimeMillis() - begin)/1000f;
        log.info("End load index of {} in {} seconds, loaded {} records", regId, seconds, images.size());
        if(service instanceof AbstractV2RegistryService && !Objects.equals(old, images)) {
            // we detect difference in image catalogs and send update event
            ((AbstractV2RegistryService)service).fireEvent(RegistryEvent.builder().action(StandardActions.UPDATE));
        }
        return images;
    }

    private ImageDescriptor loadDescriptor(String regId, String image) {
        // we use descriptor of latest image
        ImageDescriptor descriptor = null;
        String latestTag = "latest";
        try {
            descriptor = this.service.getImage(image, latestTag);
            if(descriptor == null) {
                //not any image has 'latest' tag and we may try load tags
                Tags tags = this.service.getTags(image);
                if(tags == null) {
                    log.info("Tags of image {} from registry {} is null, see above log for details.", image, regId);
                } else {
                    List<String> list = tags.getTags();
                    if(!CollectionUtils.isEmpty(list)) {
                        //order of tags is sometime random and we need to sort them
                        list.sort(ImageNameComparator.getTagsComparator());
                        latestTag = list.get(list.size() - 1);
                        descriptor = this.service.getImage(image, latestTag);
                    }
                }
            }
        } catch (Exception e) {
            // for prevent noise in log (it may happen when registry is down) we do not print stack trace
            log.info("Can not load latest image {} from registry {} with error: {}", image, regId, e.toString());
        }
        return descriptor;
    }

    @Override
    public SearchResult search(String query, int page, int count) {
        Assert.hasText(query, "query is null");
        SearchResult result = new SearchResult();
        result.setPage(0);
        result.setNumPages(1);
        result.setQuery(query);
        List<SearchResult.Result> results = new ArrayList<>();
        result.setResults(results);
        Map<String, ImageInfo> images = cache.get();
        for(String fullImageName: images.keySet()) {
            boolean match = fullImageName == null ? query == null : query != null && fullImageName.contains(query);
            if(match) {
                SearchResult.Result res = new SearchResult.Result();
                res.setName(fullImageName);
                ImageInfo ii = images.get(fullImageName);
                String description = getDescription(ii);
                res.setDescription(description);
                res.getRegistries().add(registryName);
                results.add(res);
            }
        }
        results.sort(null);
        result.setNumResults(results.size());
        result.setPageSize(result.getNumResults());
        return result;
    }

    private String getDescription(ImageInfo ii) {
        String description = null;
        ImageDescriptor descriptor = ii.getDescriptor();
        if(descriptor != null) {
            Map<String, String> labels = descriptor.getLabels();
            description = labels == null? null :  labels.get(LABEL_DESCRIPTION);
        }
        if(description == null) {
            description = "";
        }
        return description;
    }

    public void init() {
        if(ses != null) {
            //TODO we must return old cache when update in progress
            this.future = ses.scheduleWithFixedDelay(() -> cache.get(), 1000L, getTimeout(), TimeUnit.MILLISECONDS);
        }
    }

    private long getTimeout() {
        return TIMEOUT;
    }

    @Override
    public void close() throws Exception {
        ScheduledFuture<?> future = this.future;
        if(future != null) {
            future.cancel(true);
        }
    }

    @EqualsAndHashCode
    public static class ImageInfo {
        private final String name;
        private final ImageDescriptor descriptor;

        public ImageInfo(String name, ImageDescriptor descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }

        public String getName() {
            return name;
        }

        /**
         * Descriptor of latest image.
         * @return descriptor or null
         */
        public ImageDescriptor getDescriptor() {
            return descriptor;
        }
    }
}
