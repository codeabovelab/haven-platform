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

package com.codeabovelab.dm.cluman.cluster.docker.model;

/**
 * The access mode of a file system or file: <code>read-write</code> or <code>read-only</code>.
 */
public enum AccessMode {
    /** read-write */
    rw,

    /** read-only */
    ro;

    /**
     * The default {@link AccessMode}: {@link #rw}
     */
    public static final AccessMode DEFAULT = rw;

    public static AccessMode fromBoolean(boolean accessMode) {
        return accessMode ? rw : ro;
    }

    public final boolean toBoolean() {
        return this.equals(AccessMode.rw);
    }

}