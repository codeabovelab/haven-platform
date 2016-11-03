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

package com.codeabovelab.dm.cluman.model;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Standard actions over objects.
 */
public class StandardActions {
    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String START = "start";
    public static final String STOP = "stop";
    public static final String DIE = "die";
    public static final String ONLINE = "online";
    public static final String OFFLINE = "offline";

    private static final Map<String, Severity> severityMap = ImmutableMap.<String, Severity>builder()
      .put(DIE, Severity.ERROR)
      .put(OFFLINE, Severity.WARNING)
      .build();

    public static Severity toSeverity(String action) {
        Severity severity = severityMap.get(action);
        if(severity == null) {
            severity = Severity.INFO;
        }
        return severity;
    }
}
