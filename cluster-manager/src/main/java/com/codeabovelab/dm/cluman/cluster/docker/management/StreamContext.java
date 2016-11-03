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

package com.codeabovelab.dm.cluman.cluster.docker.management;

import com.google.common.util.concurrent.SettableFuture;
import lombok.Data;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 */
@Data
public class StreamContext<T> {

    private final InputStream stream;
    private final Consumer<T> watcher;
    private final SettableFuture<Boolean> interrupter = SettableFuture.create();

    public void interrupt() {
        interrupter.set(Boolean.TRUE);
    }
}
