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

package com.codeabovelab.dm.common.mb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Standard implementation of {@link ExceptionInfo } consumer
 */
public final class ExceptionInfoLogger implements Consumer<ExceptionInfo> {

    private static final ExceptionInfoLogger INSTANCE = new ExceptionInfoLogger();
    private final Logger log = LoggerFactory.getLogger(getClass());

    private ExceptionInfoLogger() {
    }

    @Override
    public void accept(ExceptionInfo ei) {
        log.error("Exception in message bus '{}' from consumer '{}' on message '{}'",
          ei.getBus().getId(),
          ei.getConsumer(),
          ei.getMessage(),
          ei.getThrowable());
    }

    public static ExceptionInfoLogger getInstance() {
        return INSTANCE;
    }
}
