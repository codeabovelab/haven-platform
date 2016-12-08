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
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryAdapter;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class DockerHubRegistryImpl extends AbstractV2RegistryService implements DockerHubRegistry {

    @Builder
    public DockerHubRegistryImpl(RegistryAdapter adapter) {
        super(adapter);
    }

    @Override
    public SearchResult search(String searchTerm, int page, int count) {
        return new SearchResult();
    }

    @Override
    public ImageCatalog getCatalog() {
        return new ImageCatalog(Collections.emptyList());
    }

    @Override
    public String toRelative(String name) {
        return name;
    }
}
