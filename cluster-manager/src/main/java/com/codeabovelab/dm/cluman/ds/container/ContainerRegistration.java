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

package com.codeabovelab.dm.cluman.ds.container;

import com.codeabovelab.dm.cluman.model.DockerContainer;
import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.codeabovelab.dm.common.kv.mapping.KvMap;
import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.codeabovelab.dm.cluman.model.ContainerBaseIface;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Map;

public class ContainerRegistration {
    private final String id;
    @KvMapping
    private Map<String, String> additionalLabels;
    private final DockerContainer.Builder container = DockerContainer.builder();
    private final Object lock = new Object();
    private DockerContainer cached;
    private KvMap<?> map;

    ContainerRegistration(ContainerStorageImpl csi, String id) {
        this.id = id;
        Assert.notNull(id, "id is null");
        this.map = csi.map;
    }

    public String getId() {
        return id;
    }

    public void setAdditionalLabels(Map<String, String> additionalLabels) {
        this.additionalLabels = additionalLabels;
    }

    public Map<String, String> getAdditionalLabels() {
        return additionalLabels == null? Collections.emptyMap() : Collections.unmodifiableMap(additionalLabels);
    }

    public void flush() {
        map.flush(id);
    }

    public DockerContainer getContainer() {
        DockerContainer dc = cached;
        if(dc == null) {
            synchronized (lock) {
                dc = cached = container.build();
            }
        }
        return dc;
    }

    public String getNode() {
        synchronized (lock) {
            return this.container.getNode();
        }
    }

    public void setNode(String node) {
        Assert.notNull(node, "Container node can not be null.");
        synchronized (lock) {
            this.container.setNode(node);
        }
    }

    public void from(ContainerBaseIface container, String node) {
        synchronized (lock) {
            String name = container.getName();
            // swarm can give container names with leading '/'
            if(name == null || name.startsWith("/")) {
                throw new IllegalArgumentException("Bad container name: " + name);
            }
            this.container.from(container);
            ContainerUtils.getFixedImageName(this.container);
            setNode(node);
            String currId = this.container.getId();
            Assert.isTrue(this.id.equals(currId), "After update container has differ id: old=" + this.id + " new=" + currId);
            this.cached = null;
        }
    }
}