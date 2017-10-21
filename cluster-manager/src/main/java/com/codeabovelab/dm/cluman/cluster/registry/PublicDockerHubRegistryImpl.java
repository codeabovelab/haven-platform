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
import com.codeabovelab.dm.common.utils.Throwables;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.springframework.web.util.UriComponentsBuilder.newInstance;

@Slf4j
public class PublicDockerHubRegistryImpl extends AbstractV2RegistryService implements DockerHubRegistry {

    private final String dockerHubSearchRegistryUrl;

    @Builder
    public PublicDockerHubRegistryImpl(RegistryAdapter adapter,
                                       String dockerHubSearchRegistryUrl) {
        super(adapter);
        this.dockerHubSearchRegistryUrl = dockerHubSearchRegistryUrl;
    }

    @Override
    public SearchResult search(String searchTerm, int page, int count) {
        try {
            // GET /v1/search?q=search_term&page=1&n=25 HTTP/1.1
            UriComponents build = getBasePath().pathSegment("search")
              .queryParam("q", searchTerm)
              .queryParam("page", page + 1 /* hub numbers pages from 1 instead of 0*/)
              .queryParam("n", count)
              .build().encode("utf-8");
            SearchResult res = getRestTemplate().getForObject(build.toUri(), SearchResult.class);
            //first page in hub will start from '1', it may confuse our api users
            res.setPage(res.getPage() - 1);
            res.getResults().forEach(r -> r.getRegistries().add(getConfig().getName()));
            return res;
        } catch (HttpStatusCodeException e) {
            // error logged internal
            processStatusCodeException(e);
            return null;
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }
    }

    private UriComponentsBuilder getBasePath() throws URISyntaxException {
        return newInstance().uri(new URI(dockerHubSearchRegistryUrl)).path("v1");
    }

    @Override
    public ImageCatalog getCatalog() {
        return new ImageCatalog(Collections.emptyList());
    }
}
