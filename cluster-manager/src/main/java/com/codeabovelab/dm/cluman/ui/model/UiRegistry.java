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

import com.codeabovelab.dm.cluman.cluster.registry.RegistryType;
import lombok.Builder;
import lombok.Data;

/**
 * DTO which is used for Registry
 */
@Data
@Builder
public final class UiRegistry {
    private final boolean disabled;
    private final boolean readOnly;
    private final String errorMessage;
    private final RegistryType registryType;
    private final String accessKey;
    private final String region;
    private final String url;
    private final String username;

}
