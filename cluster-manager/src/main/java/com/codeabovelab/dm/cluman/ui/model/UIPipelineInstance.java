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

import com.codeabovelab.dm.cluman.pipeline.instance.State;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Value
@Builder
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class UIPipelineInstance {

    private String id;
    private String pipeline;
    private State state;
    private String name;
    private String registry;
    private Map<String, UIPipelineInstanceHistory> histories;
    private Map<String, String> args;

}
