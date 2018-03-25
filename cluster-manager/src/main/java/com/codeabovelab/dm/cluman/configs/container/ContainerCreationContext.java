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

package com.codeabovelab.dm.cluman.configs.container;

import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Context of config parser
 */
@Data
@Builder
public class ContainerCreationContext {
    @NotNull
    private ImageDescriptor image;
    @NotNull
    private String imageName;
    private String cluster;
    private Optional<String> containerName;

    private final List<ContainerSource> argList = new ArrayList<>();

    public void addCreateContainerArg(ContainerSource createContainerArg) {
        argList.add(createContainerArg);
    }

}
