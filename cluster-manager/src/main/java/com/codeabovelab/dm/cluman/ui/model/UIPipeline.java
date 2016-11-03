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

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UIPipeline {

    @NotNull
    @ApiModelProperty(value = "Name of pipeline can be equal to filter")
    private String name;

    @NotNull
    @ApiModelProperty(value = "application/image which will be part of pipeline")
    private String filter;

    @NotNull
    @ApiModelProperty(value = "upstream registry")
    private String registry;

    @ApiModelProperty(value = "List of users which will get notifications")
    private List<String> recipients;

    @NotNull
    private List<UIPipelineStage> pipelineStages;

}
