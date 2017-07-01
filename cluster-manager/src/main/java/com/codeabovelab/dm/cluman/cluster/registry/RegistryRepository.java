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

package com.codeabovelab.dm.cluman.cluster.registry;

import com.codeabovelab.dm.cluman.cluster.registry.data.ImageCatalog;
import com.codeabovelab.dm.cluman.cluster.registry.data.SearchResult;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistriesConfig;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryConfig;
import com.codeabovelab.dm.cluman.model.ImageName;
import com.codeabovelab.dm.cluman.model.Severity;
import com.codeabovelab.dm.cluman.model.StandardActions;
import com.codeabovelab.dm.cluman.reconfig.ReConfigObject;
import com.codeabovelab.dm.cluman.reconfig.ReConfigurable;
import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.kv.KvStorageEvent;
import com.codeabovelab.dm.common.kv.mapping.*;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.utils.Closeables;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@ReConfigurable
public class RegistryRepository implements SupportSearch {

    private final KvMap<RegistryService> map;
    //docker hub registry
    private final DockerHubRegistry defaultRegistry;
    private final MessageBus<RegistryEvent> eventBus;
    private final RegistryFactory factory;
    private final ExecutorService executorService;

    public RegistryRepository(KvMapperFactory mapperFactory,
                              DockerHubRegistry defaultRegistry,
                              RegistryFactory factory,
                              @Qualifier(RegistryEvent.BUS) MessageBus<RegistryEvent> eventBus) {
        this.defaultRegistry = defaultRegistry;
        this.factory = factory;
        String prefix = mapperFactory.getStorage().getPrefix() + "/docker-registry/";
        this.map = KvMap.builder(RegistryService.class, RegistryConfig.class)
          .mapper(mapperFactory)
          .passDirty(true)
          .adapter(new KvMapAdapterImpl())
          .listener(this::onStorageEvent)
          .localListener(this::onLocalEvent)
          .path(prefix)
          .build();
        this.eventBus = eventBus;
        this.executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(getClass().getSimpleName() + "-eventDispatcher-%d")
                .build());
    }

    private void onLocalEvent(KvMapLocalEvent<RegistryService> e) {
        RegistryService newValue = e.getNewValue();
        if (newValue != null) {
            processNew(newValue, e.getKey());
        }
        RegistryService oldValue = e.getOldValue();
        if (oldValue != null) {
            processOld(oldValue);
        }
    }

    private void onStorageEvent(KvMapEvent<RegistryService> e) {
        KvStorageEvent.Crud action = e.getAction();
        String targetAction;
        switch (action) {
            case CREATE:
                targetAction = StandardActions.CREATE;
                break;
            case DELETE:
                targetAction = StandardActions.DELETE;
                break;
            case UPDATE:
                targetAction = StandardActions.UPDATE;
                break;
            default: return;
        }
        RegistryEvent.Builder logEvent = new RegistryEvent.Builder();
        logEvent.setAction(targetAction);
        logEvent.setName(e.getKey());
        RegistryService service = e.getValue();
        String errorMessage = null;
        if (service != null) {
            errorMessage = service.getConfig().getErrorMessage();
        }
        if (StringUtils.hasText(errorMessage)) {
            logEvent.setSeverity(Severity.ERROR);
            logEvent.setMessage(errorMessage);
        } else {
            logEvent.setSeverity(Severity.INFO);
        }
        eventBus.accept(logEvent.build());
    }

    public void init(List<RegistryConfig> configs) {
        //init from KV
        try {
            this.map.load();
//            Collection<String> list = this.map.list();
//            log.debug("Loading repositories from storage: {}", list);
//            for (String repoName : list) {
//                try {
//                    this.map.get(repoName);
//                } catch (ValidityException e) {
//                    log.error("Repository: \"{}\" is invalid, deleting.", repoName, e);
//                    //delete broken registry
//                    this.map.remove(repoName);
//                } catch (Exception e) {
//                    log.error("Can not load repository: \"{}\" from storage", repoName, e);
//                }
//            }
        } catch (Exception e) {
            log.error("Can not list repositories in storage, due to error.", e);
        }

        //init from config
        for (RegistryConfig config : configs) {
            factory.complete(config);
            String name = config.getName();
            Assert.hasText(name, "Config should have non empty name.");
            RegistryService registryService = factory.createRegistryService(config);
            try {
                internalRegister(registryService);
            } catch (Exception e) {
                log.error("Can not register repository: \"{}\" ", registryService, e);
            }
        }
    }

    public void register(RegistryService registryService) {
        RegistryConfig config = registryService.getConfig();
        Assert.notNull(config, "Config should not be null!");
        internalRegister(registryService);
    }

    private void internalRegister(RegistryService service) {
        String name = service.getConfig().getName();
        checkNotDefaultName(name);
        ExtendedAssert.matchId(name, "registry name");
        map.put(name, service);
    }

    private void processOld(RegistryService old) {
        Closeables.closeIfCloseable(old);
    }

    private void processNew(RegistryService service, String name) {
        if (service instanceof AbstractV2RegistryService) {
            ((AbstractV2RegistryService) service).setEventConsumer(this::dispatchEvent);
        }
        if (service instanceof InitializingBean) {
            try {
                ((InitializingBean) service).afterPropertiesSet();
            } catch (Exception e) {
                log.error("Can not init repository: \"{}\"", name, e);
            }
        }
    }

    private void checkNotDefaultName(String name) {
        if(defaultRegistry.getConfig().getName().equals(name)) {
            throw new IllegalArgumentException("Can not override default registry.");
        }
    }

    private void dispatchEvent(RegistryEvent event) {
        // we execute events in service only when event came from registry
        this.executorService.execute(() -> {
            this.eventBus.accept(event);
        });
    }

    public void unRegister(String name) {
        checkNotDefaultName(name);
        map.remove(name);
    }

    public List<ImageCatalog> getCatalog(Collection<String> names) {

        if (CollectionUtils.isEmpty(names)) {
            names = getAvailableRegistries();
        }
        return names.stream()
                .filter(f -> !getByName(f).getConfig().isDisabled())
                .filter(f -> !StringUtils.hasText(getByName(f).getConfig().getErrorMessage()))
                .filter(f -> !(getByName(f) instanceof DockerHubRegistry))
                .map(n -> {
                    ImageCatalog imageCatalog = getByName(n).getCatalog();
                    imageCatalog.setName(n);
                    return imageCatalog;
                }).collect(Collectors.toList());
    }

    public Collection<String> getAvailableRegistries() {
        return ImmutableSet.<String>builder()
          .addAll(map.list())
          .add(defaultRegistry.getConfig().getName())
          .build();
    }

    /**
     * Returns registry name even we can't operate with it (for downloaded images for example)
     *
     * @param imageName
     * @return never returns null
     */
    public String resolveRegistryNameByImageName(String imageName) {
        RegistryService registryByImageName = getRegistryByImageName(imageName);
        if (registryByImageName != null) {
            return registryByImageName.getConfig().getName();
        } else {
            return ContainerUtils.getRegistryPrefix(imageName);
        }
    }

    /**
     * returns registry by image, can return null
     * @param imageName
     * @return
     */
    public RegistryService getRegistryByImageName(String imageName) {
        RegistryService registryService;
        if (!StringUtils.hasText(imageName)) {
            return wrapDefault(imageName);
        }
        String registryPrefix = ContainerUtils.getRegistryPrefix(imageName);
        RegistryService byName = getByName(registryPrefix);
        if (byName != null) {
            registryService = byName;
        } else if (!ImageName.isRegistry(registryPrefix)) {
            registryService = wrapDefault(registryPrefix);
        } else {
            //we don't have such registry
            return null;
        }

        if (registryService.getConfig().isDisabled() || StringUtils.hasText(registryService.getConfig().getErrorMessage())) {
            return new DisabledRegistryServiceWrapper(registryService);
        }
        return registryService;
    }

    private RegistryService wrapDefault(String registry) {
        return new DockerHubRegistryServiceWrapper(defaultRegistry, registry);
    }

    RegistryService getDefaultRegistry() {
        return defaultRegistry;
    }

    /**
     * Get registry by name
     *
     * @param registryName
     * @return
     */
    public RegistryService getByName(String registryName) {
        RegistryService service = null;
        if (registryName != null && !Objects.equals(defaultRegistry.getConfig().getName(), registryName)) {
            service = map.get(registryName);
        }
        if (service == null) {
            service = defaultRegistry;
        }
        return service;
    }

    @Override
    public SearchResult search(String query, final int page, final int size) {
        RegistrySearchHelper rsh = new RegistrySearchHelper(query, page, size);
        for (RegistryService service : map.values()) {
            rsh.search(service);
        }
        rsh.search(defaultRegistry);
        return rsh.collect();
    }

    @ReConfigObject
    private RegistriesConfig getConfig() {
        RegistriesConfig rsc = new RegistriesConfig();
        map.forEach((k, v) -> {
            RegistryConfig config = v.getConfig();
            RegistryConfig exported = config.clone();
            exported.setName(k);
            rsc.getRegistries().add(exported);
        });
        return rsc;
    }

    @ReConfigObject
    private void setConfig(RegistriesConfig rc) {
        List<RegistryConfig> registries = rc.getRegistries();
        for (RegistryConfig config : registries) {
            RegistryService registryService = factory.createRegistryService(config);
            register(registryService);
        }

    }

    private class KvMapAdapterImpl implements KvMapAdapter<RegistryService> {
        @Override
        public Object get(String key, RegistryService source) {
            return source.getConfig();
        }

        @Override
        public RegistryService set(String key, RegistryService source, Object value) {
            RegistryConfig config = (RegistryConfig) value;
            return factory.createRegistryService(config);
        }
    }
}
