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

import java.io.File;
import java.util.Map;

/**
 * Parses settings and adds result to context
 */
public interface Parser {
    void parse(String fileName, ContainerCreationContext context);
    void parse(File file, ContainerCreationContext context);
    void parse(Map<String, Object> map, ContainerCreationContext context);
    void parse(Map<String, Object> map, ContainerSource arg);
}
