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

package com.codeabovelab.dm.cluman.pipeline.arg;

import com.codeabovelab.dm.cluman.pipeline.schema.Action;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 */
@Data
@Builder
public class PipelineStageSchemaArg {

    private final String name;
    private final List<String> clusters;
    //TODO: add sequence
    private final List<Action> actions;
    private final String tagSuffix;
    private final Boolean autoDeployLatest;
    private final List<String> recipients;

}
