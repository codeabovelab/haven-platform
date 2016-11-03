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

import com.codeabovelab.dm.cluman.cluster.registry.data.SearchResult;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryAdapter;
import lombok.Builder;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.ScheduledExecutorService;

/**
 * REST implementation of RegistryService
 */
public class RegistryServiceImpl extends AbstractV2RegistryService implements AutoCloseable, InitializingBean {

    private final SearchIndex searchIndex;

    @Builder
    public RegistryServiceImpl(RegistryAdapter adapter,
                               ScheduledExecutorService scheduledExecutorService) {
        super(adapter);
        this.searchIndex = new SearchIndex(this, scheduledExecutorService);
    }

    @Override
    public SearchResult search(String searchTerm, int page, int count) {
        return searchIndex.search(searchTerm, page, count);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        //test of registry
        checkHealth();
        searchIndex.init();
    }

    @Override
    public void close() throws Exception {
        this.searchIndex.close();
    }
}
