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


import com.codeabovelab.dm.cluman.cluster.registry.data.ImageCatalog;
import com.codeabovelab.dm.cluman.cluster.registry.data.SearchResult;
import com.codeabovelab.dm.cluman.cluster.registry.data.Tags;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryCredentials;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryConfig;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import com.codeabovelab.dm.common.cache.DefineCache;
import org.springframework.cache.annotation.Cacheable;

/**
 * Docker registry read-only API
 */
public interface RegistryService extends SupportSearch {

    /**
     * Retrieve a sorted, json list of repositories available in the registry.
     * @return ImageCatalog
     */
    @Cacheable("ImageCatalog")
    @DefineCache(expireAfterWrite = 60_000)
    ImageCatalog getCatalog();

    /**
     * Fetch the tags under the repository identified by name.
     * @param name
     * @return tags with sorted list or null when image not found
     */
    @Cacheable("Tags")
    @DefineCache(expireAfterWrite = 60_000)
    Tags getTags(String name);

    /**
     * Create Image description from manifest
     * @param name Name of the target repository.
     * @param reference Tag or digest of the target manifest.
     * @return Image or null when image not found
     */
    @Cacheable("ImageDescriptor")
    @DefineCache(expireAfterWrite = 60_000)
    ImageDescriptor getImage(String name, String reference);

    /**
     * simplified method
     * @param fullImageName
     * @return
     */
    @Cacheable("ImageDescriptor")
    @DefineCache(expireAfterWrite = 60_000)
    ImageDescriptor getImage(String fullImageName);
    /**
     * Delete the manifest identified by name and reference where reference can be a tag or digest.
     * @param name
     * @param reference
     */
    void deleteTag(String name, String reference);

    /**
     * Registry service configuration
     * @return
     */
    RegistryConfig getConfig();

    /**
     * check health of registry
     * @return
     */
    boolean checkHealth();

    @Override
    @Cacheable("SearchResult")
    @DefineCache(expireAfterWrite = 60_000)
    SearchResult search(String searchTerm, int page, int count);

    /**
     * Credentials for login into registry for docker service.
     * @return
     */
    RegistryCredentials getCredentials();
}
