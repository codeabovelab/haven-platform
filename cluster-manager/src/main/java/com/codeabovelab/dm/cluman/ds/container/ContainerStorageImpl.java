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

import com.codeabovelab.dm.common.kv.DeleteDirOptions;
import com.codeabovelab.dm.common.kv.KvUtils;
import com.codeabovelab.dm.common.kv.WriteOptions;
import com.codeabovelab.dm.common.kv.mapping.KvMap;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.cluman.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ContainerStorageImpl implements ContainerStorage, InitializingBean {

    private final KvMapperFactory kvmf;

    private final KvMap<ContainerRegistration> map;
    private final String prefix;

    @Autowired
    public ContainerStorageImpl(KvMapperFactory kvmf) {
        this.kvmf = kvmf;
        this.prefix = kvmf.getStorage().getPrefix() + "/containers/";
        this.map = KvMap.builder(ContainerRegistration.class)
          .factory(kvmf)
          .path(prefix)
          .build();
    }

    @Override
    public void afterPropertiesSet() {
        this.kvmf.getStorage().setdir(this.prefix, WriteOptions.builder().build());
    }


    void deleteContainer(String id) {
        try {
            final String path = KvUtils.join(prefix, id);
            kvmf.getStorage().deletedir(path, DeleteDirOptions.builder().recursive(true).build());
        } catch (Exception e) {
            log.error("Can't delete container", e);
        }
        ContainerRegistration cr = map.remove(id);
        if(cr != null) {
            ContainerBase cb = cr.getContainer();
            log.info("Container remove: {} '{}', of '{}'", cr.getId(), cb.getName(), cb.getImage());
        }
    }

    @Override
    public List<ContainerRegistration> getContainers() {
        return new ArrayList<>(map.values());
    }

    @Override
    public ContainerRegistration getContainer(String id) {
        return map.get(id);
    }

    @Override
    public ContainerRegistration findContainer(String name) {
        ContainerRegistration cr = map.get(name);
        if(cr == null) {
            cr = map.values().stream().filter((c) -> {
                return c.getId().startsWith(name) || c.getContainer().getName().equals(name);
            }).findAny().orElse(null);
        }
        return cr;
    }

    @Override
    public List<ContainerRegistration> getContainersByNode(String nodeName) {
        return map.values()
          .stream()
          .filter(c -> c.getNode().equals(nodeName))
          .collect(Collectors.toList());
    }

    Set<String> getContainersIdsByNode(String nodeName) {
        return map.values()
          .stream()
          .filter(c -> c.getNode().equals(nodeName))
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
    public ContainerRegistration getOrCreateContainer(ContainerBaseIface container, String node) {
        return getOrCreateContainer(container.getId(), cr -> cr.from(container, node));
    }

    ContainerRegistration getOrCreateContainer(String id, Consumer<ContainerRegistration> onCreate) {
        return map.computeIfAbsent(id, s -> {
            ContainerRegistration registration = new ContainerRegistration(this.map, id);
            onCreate.accept(registration);
            registration.flush();
            ContainerBase cb = registration.getContainer();
            log.info("Create container: {} '{}', of '{}'", registration.getId(), cb.getName(), cb.getImage());
            return registration;
        });
    }

    void remove(Set<String> ids) {
        ids.forEach(this::deleteContainer);
    }

    void removeNodeContainers(String nodeName) {
        Set<String> nodeIds = getContainersIdsByNode(nodeName);
        remove(nodeIds);
    }
}
