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

import com.codeabovelab.dm.cluman.cluster.docker.model.DockerEvent;
import com.google.common.util.concurrent.SettableFuture;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Builder
@Data
public class GetEventsArg implements WithInterrupter {

    private final Consumer<DockerEvent> watcher;

    private final Map<String, List<String>> filters;
    // service accept time in specific string format, we must not deliver this to user
    private final Long since;

    private final Long until;

    private final SettableFuture<Boolean> interrupter = SettableFuture.create();
}
