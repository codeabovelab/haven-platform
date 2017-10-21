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

package com.codeabovelab.dm.cluman.cluster.registry.data;

import com.codeabovelab.dm.cluman.cluster.registry.ImageNameComparator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Response Items:
 * - num_pages - Total number of pages returned by query
 * - num_results - Total number of results returned by query
 * - results - List of results for the current page
 * - page_size - How many results returned per page
 * - query - Your search term - page - Current page number
 * <p>
 * {"num_pages": 1,
 * "num_results": 3,
 * "results" : [
 * {"name": "ubuntu", "description": "An ubuntu image..."},
 * {"name": "centos", "description": "A centos image..."},
 * {"name": "fedora", "description": "A fedora image..."}
 * ],
 * "page_size": 25,
 * "query":"search_term",
 * "page": 1
 * }
 */
@Data
public class SearchResult {

    @JsonProperty("num_pages")
    private int numPages;
    @JsonProperty("num_results")
    private int numResults;
    @JsonProperty("page_size")
    private int pageSize;
    private int page;
    private String query;
    private List<Result> results;

    /**
     * "is_automated": false,
     * "name": "ubuntu",
     * "is_trusted": false,
     * "is_official": true,
     * "star_count": 4100,
     * "description": "Ubuntu is a Debian-based Linux operating system based on free software."
     */
    @Data
    public static class Result implements Comparable<Result> {
        private String name;
        private String description;
        private boolean isAutomated;
        private boolean isTrusted;
        private boolean isOfficial;
        private Integer starCount;
        @JsonIgnore
        private final Set<String> registries = new HashSet<>(1);

        @Override
        public int compareTo(Result o) {
            return ImageNameComparator.STRING.compare(getName(), o.getName());
        }
    }

}
