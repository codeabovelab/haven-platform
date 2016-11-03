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

package com.codeabovelab.dm.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A util for closeables (you can se many similar utilites in guava and apache common-io, but we need some logging)
 */
public final class Closeables {

    private static final Logger LOG = LoggerFactory.getLogger(Closeables.class);

    /**
     * silently close closeable, catch any exception and write it to log
     * @param autoCloseable or null
     */
    public static void close(AutoCloseable autoCloseable) {
        if(autoCloseable == null) {
            return;
        }
        try {
            autoCloseable.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("On close.", e);
        }
    }

    /**
     * Close specified object if it is instance of AutoCloseable
     * @param mayBeCloseable
     */
    public static void closeIfCloseable(Object mayBeCloseable) {
        if(mayBeCloseable instanceof AutoCloseable) {
            close((AutoCloseable) mayBeCloseable);
        }
    }
}
