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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper utility for search in multiple registry. May be reused.
 */
@Slf4j
public class RegistrySearchHelper {

    /**
     * Max count of pages. But now we show only first page.
     */
    private static final int MAX_PAGES = 1;
    /**
     *  Max count of results on one page.
     */
    private static final int MAX_PAGE_SIZE = 100;


    private final Map<String, SearchResult.Result> results = new LinkedHashMap<>();
    private final String query;
    private final SearchResult res;

    public RegistrySearchHelper(String query, int page, int pageSize) {
        this.query = query;
        this.res = new SearchResult();
        res.setNumPages(1);
        res.setPage(0);
        res.setQuery(query);
    }

    public void search(RegistryService service) {
        //service may return small peace of result instead of all
        // but we do not want to make too many requests
        int count = MAX_PAGES;
        int page  = 0;
        int pageSize  = MAX_PAGE_SIZE;
        while(count > 0) {
            count--;
            SearchResult tmp = service.search(query, page, pageSize);
            if(tmp == null) {
                log.warn("Search \"{}\" on {} will ended with error, see log", query, service.getConfig().getName());
                return;
            }
            List<SearchResult.Result> localResults = tmp.getResults();
            if(localResults != null) {
                for(SearchResult.Result result: localResults) {
                    SearchResult.Result exists = results.putIfAbsent(result.getName(), result);
                    if(exists != null) {
                        exists.getRegistries().addAll(result.getRegistries());
                    }
                }
            }
            page = tmp.getPage() + 1;
            if(page >= tmp.getNumPages()) {
                return;
            }
            pageSize = tmp.getPageSize();
            if(pageSize <= 0) {
                pageSize = MAX_PAGE_SIZE;
            }
        }
    }

    public SearchResult collect() {
        res.setNumResults(results.size());
        res.setPageSize(res.getNumResults());
        res.setResults(new ArrayList<>(results.values()));
        // clear for reuse
        results.clear();
        return res;
    }
}
