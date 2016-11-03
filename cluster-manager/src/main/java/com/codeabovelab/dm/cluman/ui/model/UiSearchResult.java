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

package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.cluman.cluster.registry.data.SearchResult;
import lombok.Data;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
@Data
public class UiSearchResult {
    private int totalPages;
    private int totalResults;
    private int pageSize;
    private int page;
    private String query;
    private List<Result> results;

    @Data
    public static class Result {
        private String name;
        private String description;
        private Collection<String> registries;

        public static Result from(SearchResult.Result res) {
            Result ui = new Result();
            ui.setName(res.getName());
            ui.setDescription(res.getDescription());
            ui.setRegistries(res.getRegistries());
            return ui;
        }
    }

    public static UiSearchResult from(SearchResult res) {
        UiSearchResult ui = new UiSearchResult();
        ui.setTotalPages(res.getNumPages());
        ui.setTotalResults(res.getNumResults());
        ui.setPageSize(res.getPageSize());
        ui.setPage(res.getPage());
        ui.setQuery(res.getQuery());
        ui.setResults(res.getResults().stream().map(Result::from).collect(Collectors.toList()));
        return ui;
    }

}
