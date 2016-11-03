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

package com.codeabovelab.dm.cluman.cluster.docker.management.argument;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TagImageArg {

    /**
     * image name w/o version
     */
    private final String imageName;
    private final String cluster;
    private final String repository;
    /**
     * new tag
     */
    private final String newTag;
    private final String currentTag;
    private final Boolean force;
    private final Boolean remote;

}
