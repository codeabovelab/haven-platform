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

import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UiImage extends UiImageData {
    private final String name;
    private final String registry;
    private final List<String> clusters = new ArrayList<>();

    public UiImage(String id, String name) {
        super(id);
        this.name = name;
        this.registry = ContainerUtils.getRegistryName(this.name);
    }
}
