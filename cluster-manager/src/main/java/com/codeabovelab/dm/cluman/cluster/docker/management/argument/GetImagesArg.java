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

// do not remove @Builder from here! (wayerr)
// do not add @AllArgsConstructor
// because we need to add label based filter api for this
@Data
@Builder
public class GetImagesArg {

    /**
     * Instance which only has all=true
     */
    public static GetImagesArg ALL = GetImagesArg.builder().all(true).build();
    /**
     * Instance which only has all=false
     */
    public static GetImagesArg NOT_ALL = GetImagesArg.builder().all(false).build();

    private final boolean all;
    private final String name;
}
