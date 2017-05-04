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
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.common.utils.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class ContainerStorageImpl implements ContainerStorage {

    final KvMap<ContainerRegistration> map;
    final ScheduledExecutorService executorService;

    @Autowired
    public ContainerStorageImpl(KvMapperFactory kvmf) {
        String prefix = kvmf.getStorage().getPrefix() + "/containers/";
        this.map = KvMap.builder(ContainerRegistration.class)
          .mapper(kvmf)
          .path(prefix)
          .factory((key, type) -> new ContainerRegistration(this, key))
          .build();
        this.executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat(getClass().getSimpleName() + "-scheduled-%d")
          .setUncaughtExceptionHandler(Throwables.uncaughtHandler(log))
          .build());
    }

    @PostConstruct
    public void postConstruct() {
        this.map.load();
    }

    @PreDestroy
    private void preDestroy() {
        this.executorService.shutdown();
    }

    @Override
    public void deleteContainer(String id) {
        ContainerRegistration cr = map.remove(id);
        if(cr != null) {
            log.info("Container remove: {} ", cr.forLog());
            cr.close();
        }
    }

    @Override
    public List<ContainerRegistration> getContainers() {
        return ImmutableList.copyOf(map.values());
    }

    @Override
    public ContainerRegistration getContainer(String id) {
        return map.get(id);
    }

    @Override
    public ContainerRegistration findContainer(String name) {
        ContainerRegistration cr = map.get(name);
        if(cr == null) {
            cr = map.values().stream().filter((item) -> {
                DockerContainer container = item.getContainer();
                return container != null && (item.getId().startsWith(name) || Objects.equals(container.getName(), name));
            }).findAny().orElse(null);
        }
        return cr;
    }

    @Override
    public List<ContainerRegistration> getContainersByNode(String nodeName) {
        return containersByNode(nodeName)
          .collect(Collectors.toList());
    }

    private Stream<ContainerRegistration> containersByNode(String nodeName) {
        return map.values()
          .stream()
          .filter(c -> Objects.equals(c.getNode(), nodeName));
    }

    Set<String> getContainersIdsByNode(String nodeName) {
        return containersByNode(nodeName)
          .map(ContainerRegistration::getId)
          .collect(Collectors.toSet());
    }

    /**
     * Get or create container.
     *
     * @param container
     * @param node
     * @return
     */
    @Override
    public ContainerRegistration updateAndGetContainer(ContainerBaseIface container, String node) {
        ContainerRegistration cr = map.computeIfAbsent(container.getId(), s -> new ContainerRegistration(this, s));
        cr.from(container, node);
        log.info("Update container: {}", cr.forLog());
        return cr;
    }

    void remove(Set<String> ids) {
        ids.forEach(this::deleteContainer);
    }

    void removeNodeContainers(String nodeName) {
        Set<String> nodeIds = getContainersIdsByNode(nodeName);
        remove(nodeIds);
    }
}
