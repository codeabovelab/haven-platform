/*
 * Copyright 2017 Code Above Lab LLC
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

package com.codeabovelab.dm.cluman.cluster.docker.management.argument;

import lombok.Data;

import java.util.Map;

/**
 * Planned for 'filters':
 * <pre>
 * JSON encoded value of the filters (a map[string][]string) to process on the volumes list. Available filters:
     name=<volume-name> Matches all or part of a volume name.
     dangling=<boolean> When set to true (or 1), returns all volumes that are not in use by a container.
              When set to false (or 0), only volumes that are in use by one or more containers are returned.
     driver=<volume-driver-name> Matches all or part of a volume driver name.
 * </pre>
 */
@Data
public class GetVolumesArg {
    private Map<String, String> filters;
}
