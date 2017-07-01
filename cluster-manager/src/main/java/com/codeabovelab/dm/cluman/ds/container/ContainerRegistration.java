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

import com.codeabovelab.dm.cluman.model.ContainerBaseIface;
import com.codeabovelab.dm.cluman.model.DockerContainer;
import com.codeabovelab.dm.common.kv.mapping.KvMap;
import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.codeabovelab.dm.common.utils.RescheduledTask;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ContainerRegistration {
    private final String id;
    private final RescheduledTask resheduleTask;
    @KvMapping
    private Map<String, String> additionalLabels;
    /**
     * We persist container for detect cases when it quietly removed
     */
    @KvMapping
    private DockerContainer.Builder container;
    private final Object lock = new Object();
    private DockerContainer cached;
    private KvMap<?> map;

    ContainerRegistration(ContainerStorageImpl csi, String id) {
        this.id = id;
        Assert.notNull(id, "id is null");
        this.container = DockerContainer.builder().id(id);
        this.map = csi.map;
        this.resheduleTask = RescheduledTask.builder()
          .maxDelay(10L, TimeUnit.SECONDS)
          .service(csi.executorService)
          .runnable(this::flush)
          .build();
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

    public void scheduleFlush() {
        this.resheduleTask.schedule(10L, TimeUnit.SECONDS);
    }
    public void flush() {
        map.flush(id);
    }

    /**
     * Return container from its registration, when container invalid - return null.
     * @return null when container is invalid
     */
    public DockerContainer getContainer() {
        DockerContainer dc = cached;
        if(dc == null) {
            synchronized (lock) {
                dc = cached = container.id(id).build();
            }
        }
        return dc;
    }

    public String getNode() {
        synchronized (lock) {
            return this.container.getNode();
        }
    }

    /**
     * Note that this method do 'flush' in different thread.
     * @param modifier callback which can modify container, and must not block thread.
     */
    public void modify(Consumer<DockerContainer.Builder> modifier) {
        synchronized (lock) {
            modifier.accept(this.container);
            validate();
            this.cached = null;
        }
        scheduleFlush();
    }

    public void from(ContainerBaseIface container, String node) {
        modify((cb) -> {
            synchronized (lock) {
                this.container.from(container).setNode(node);
            }
        });
    }

    private void validate() {
        String name = this.container.getName();
        // swarm can give container names with leading '/'
        if(name != null && name.startsWith("/")) {
            throw new IllegalArgumentException("Bad container name: " + name);
        }
        String currId = this.container.getId();
        Assert.isTrue(this.id.equals(currId), "After update container has differ id: old=" + this.id + " new=" + currId);
    }

    protected String forLog() {
        synchronized (lock) {
            return new StringBuilder().append(id).append(" \'")
                    .append(container.getName()).append("\' of \'")
                    .append(container.getImage()).append('\'')
                    .toString();
        }
    }

    protected void close() {
        resheduleTask.close();
    }
}