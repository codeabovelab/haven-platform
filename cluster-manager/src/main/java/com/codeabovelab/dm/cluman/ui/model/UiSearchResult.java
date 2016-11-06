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
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
@Value
@Builder
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class UiSearchResult {
    private final int totalPages;
    private final int totalResults;
    private final int pageSize;
    private final int page;
    private final String query;
    private final List<Result> results;

    @Value
    @Builder
    @AllArgsConstructor(onConstructor = @__(@JsonCreator))
    public static class Result {
        private final String name;
        private final String description;
        private final Collection<String> registries;

        public static Result from(SearchResult.Result res) {
            return Result.builder()
                    .name(res.getName())
                    .description(res.getDescription())
                    .registries(res.getRegistries())
                    .build();
        }
    }

    public static UiSearchResult from(SearchResult res) {
        return UiSearchResult.builder()
                .totalPages(res.getNumPages())
                .totalResults(res.getNumResults())
                .pageSize(res.getPageSize())
                .page(res.getPage())
                .query(res.getQuery())
                .results(res.getResults().stream().map(Result::from).collect(Collectors.toList()))
                .build();
    }

}
