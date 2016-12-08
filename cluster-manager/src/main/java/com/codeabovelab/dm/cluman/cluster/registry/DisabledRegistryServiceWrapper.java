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
import com.codeabovelab.dm.cluman.cluster.registry.data.Manifest;
import com.codeabovelab.dm.cluman.cluster.registry.data.SearchResult;
import com.codeabovelab.dm.cluman.cluster.registry.data.Tags;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryCredentials;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryConfig;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

/**
 * Docker registry read-only API
 */
@Slf4j
@RequiredArgsConstructor
public class DisabledRegistryServiceWrapper implements RegistryService {

    private final RegistryService registryService;

    public ImageCatalog getCatalog() {
        logWarn();
        return null;
    }

    private void logWarn() {
        log.error("Registry is disabled {} ", getConfig());
    }

    public Tags getTags(String name) {
        logWarn();
        return null;
    }

    public Manifest getManifest(String name, String reference) {
        logWarn();
        return null;
    }

    public ImageDescriptor getImage(String name, String reference) {
        logWarn();
        return null;
    }

    @Override
    public ImageDescriptor getImage(String fullImageName) {
        logWarn();
        return null;
    }

    @Override
    public void deleteTag(String name, String reference) {
        logWarn();
        throw new IllegalStateException("Registry is disabled");
    }

    @Override
    public RegistryConfig getConfig() {
        return registryService.getConfig();
    }

    @Override
    public RegistryCredentials getCredentials() {
        return registryService.getCredentials();
    }

    @Override
    public boolean checkHealth() {
        logWarn();
        return false;
    }

    @Override
    public String toRelative(String name) {
        return registryService.toRelative(name);
    }

    @Override
    public SearchResult search(String searchTerm, int page, int count) {
        SearchResult result = new SearchResult();
        result.setPage(0);
        result.setNumPages(1);
        result.setResults(Collections.emptyList());
        result.setQuery(searchTerm);
        result.setPageSize(count);
        result.setNumResults(0);
        return result;
    }
}
