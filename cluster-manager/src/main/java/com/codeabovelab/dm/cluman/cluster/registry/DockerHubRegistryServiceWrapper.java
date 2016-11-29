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
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.util.StringUtils.hasText;

/**
 * Docker registry read-only API
 */
@Slf4j
@Builder
class DockerHubRegistryServiceWrapper implements RegistryService, DockerHubRegistry {

    private final DockerHubRegistry dockerHubRegistry;
    private final String registryName;

    public ImageCatalog getCatalog() {
        return dockerHubRegistry.getCatalog();
    }

    public Tags getTags(String imageName) {
        return dockerHubRegistry.getTags(merge(registryName, imageName));
    }

    public ImageDescriptor getImage(String name, String reference) {
        return dockerHubRegistry.getImage(merge(registryName, name), reference);
    }

    @Override
    public void deleteTag(String name, String reference) {
        dockerHubRegistry.deleteTag(merge(registryName, name), reference);
    }

    @Override
    public RegistryConfig getConfig() {
        return dockerHubRegistry.getConfig();
    }

    @Override
    public RegistryCredentials getCredentials() {
        return dockerHubRegistry.getCredentials();
    }

    @Override
    public boolean checkHealth() {
        return dockerHubRegistry.checkHealth();
    }

    @Override
    public SearchResult search(String searchTerm, int page, int count) {
        return dockerHubRegistry.search(searchTerm, page, count);
    }

    private static String merge(String registryName, String name) {
        return (hasText(registryName) ? registryName : "library") + "/" + name;
    }
}
