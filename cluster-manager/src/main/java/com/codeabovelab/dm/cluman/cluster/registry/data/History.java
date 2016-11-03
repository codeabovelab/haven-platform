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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * History stores unstructured v1 compatibility information
 * history data temporary disabled because v1Compatibility is not json but string
 */
public class History {

    private final static String V1 = "v1Compatibility";

    private String layer;

    @JsonProperty(V1)
    public String getHistory() {
        return layer;
    }

    @JsonProperty(V1)
    public void setHistory(String history) {
        this.layer = history;
    }
}
