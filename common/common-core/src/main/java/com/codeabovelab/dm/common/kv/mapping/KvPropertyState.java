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

package com.codeabovelab.dm.common.kv.mapping;

/**
 * State of property
 */
class KvPropertyState {
    private final KvProperty property;
    /**
     * Internal storage value, index of last modification.
     */
    private volatile long storageIndex;
    private volatile boolean modified;

    public KvPropertyState(KvProperty property) {
        this.property = property;
    }

    public KvProperty getProperty() {
        return property;
    }

    public long getStorageIndex() {
        return storageIndex;
    }

    public void setStorageIndex(long storageIndex) {
        this.storageIndex = storageIndex;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }
}
