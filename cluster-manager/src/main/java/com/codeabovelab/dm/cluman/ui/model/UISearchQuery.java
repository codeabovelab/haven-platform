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

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Value;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@Value
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class UISearchQuery {

    @ApiModelProperty(value = "Example: \"health.sysCpuLoad < 0.99 && health.healthy == true || health.sysMemUsed > 1000\"")
    private final String criterias;

    private final List<SearchOrder> orders;

    @Min(value = 1)
    private final int size;

    @Min(value = 0)
    private final int page;

    public enum SortOrder {
        ASC, DESC
    }

    public enum Operation {
        GREATER, LESS, EQUAL
    }

    @Value
    @AllArgsConstructor(onConstructor = @__(@JsonCreator))
    public static class SearchOrder {
        @NotNull
        private final String field;
        @NotNull
        private final SortOrder order;

    }
}
