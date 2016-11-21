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

import com.codeabovelab.dm.cluman.cluster.filter.Filter;
import com.codeabovelab.dm.cluman.cluster.registry.data.ImageCatalog;
import com.codeabovelab.dm.cluman.cluster.registry.data.SearchResult;
import com.codeabovelab.dm.cluman.cluster.registry.data.Tags;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistriesConfig;
import com.codeabovelab.dm.cluman.cluster.registry.model.RegistryConfig;
import com.codeabovelab.dm.cluman.validate.ExtendedAssert;
import com.codeabovelab.dm.common.kv.mapping.KvClassMapper;
import com.codeabovelab.dm.common.kv.mapping.KvMapperFactory;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import com.codeabovelab.dm.cluman.model.Severity;
import com.codeabovelab.dm.cluman.model.StandardActions;
import com.codeabovelab.dm.cluman.reconfig.ReConfigObject;
import com.codeabovelab.dm.cluman.reconfig.ReConfigurable;
import com.codeabovelab.dm.common.validate.ValidityException;
import com.codeabovelab.dm.common.mb.MessageBus;
import com.codeabovelab.dm.common.utils.Closeables;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@ReConfigurable
public class RegistryRepository implements SupportSearch {

    // we need save order of map elements, so use LinkedHashMap, it affects a search results order
    private final Map<String, RegistryService> registryServiceMap = Collections.synchronizedMap(new LinkedHashMap<>());
    //docker hub registry
    private final DockerHubRegistry defaultRegistry;
    private final KvClassMapper<RegistryConfig> classMapper;
    private final MessageBus<RegistryEvent> eventBus;
    private final RegistryFactory factory;
    private final ExecutorService executorService;

    public RegistryRepository(KvMapperFactory classMapper,
                              DockerHubRegistry defaultRegistry,
                              RegistryFactory factory,
                              @Qualifier(RegistryEvent.BUS) MessageBus<RegistryEvent> eventBus) {
        this.defaultRegistry = defaultRegistry;
        this.factory = factory;
        String prefix = classMapper.getStorage().getDockMasterPrefix() + "/docker-registry/";
        this.classMapper = classMapper.createClassMapper(prefix, RegistryConfig.class);
        this.eventBus = eventBus;
        this.executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat(getClass().getSimpleName() + "-eventDispatcher-%d")
          .build());
    }

    public void init(List<RegistryConfig> configs) {
        //init from config
        Set<String> unpersisted = new HashSet<>();
        for (RegistryConfig config : configs) {
            factory.complete(config);
            String name = config.getName();
            Assert.hasText(name, "Config should have non empty name.");
            unpersisted.add(name);
            RegistryService registryService = factory.createRegistryService(config);
            try {
                internalRegister(registryService);
                fireRegistryAddedEvent(config);
            } catch (Exception e) {
                log.error("Can not register repository: \"{}\" ", registryService, e);
            }
        }

        //init from KV
        try {
            List<String> list = this.classMapper.list();
            log.debug("Loading repositories from storage: {}", list);
            for (String repoName : list) {
                try {
                    RegistryConfig config = this.classMapper.load(repoName);
                    RegistryService service = factory.createRegistryService(config);
                    //note that here we may replace existing services from config
                    //   but it need for cases when somebody edit service through api
                    internalRegister(service);
                    unpersisted.remove(repoName);
                    fireRegistryAddedEvent(service.getConfig());
                } catch (ValidityException e) {
                    log.error("Repository: \"{}\" is invalid, deleting.", repoName, e);
                    //delete broken registry
                    classMapper.delete(repoName);
                } catch (Exception e) {
                    log.error("Can not load repository: \"{}\" from storage", repoName, e);
                }
            }
        } catch(Exception e) {
            log.error("Can not list repositories in storage, due to error.", e);
        }
        //save newly added registries
        for(String name: unpersisted) {
            RegistryService service = registryServiceMap.get(name);
            if(service == null) {
                continue;
            }
            RegistryConfig config = service.getConfig();
            classMapper.save(name, config);
        }
    }

    public void register(RegistryService registryService) {
        RegistryConfig config = registryService.getConfig();
        Assert.notNull(config, "Config should not be null!");
        String name = config.getName();
        internalRegister(registryService);
        classMapper.save(name, registryService.getConfig());
        fireRegistryAddedEvent(config);
    }

    private void internalRegister(RegistryService service) {
        String name = service.getConfig().getName();
        ExtendedAssert.matchId(name, "registry name");
        RegistryService old = registryServiceMap.put(name, service);
        if(old != service) {
            if(service instanceof AbstractV2RegistryService) {
                ((AbstractV2RegistryService)service).setEventConsumer(this::dispatchEvent);
            }
            if(service instanceof InitializingBean) {
                try {
                    ((InitializingBean)service).afterPropertiesSet();
                } catch (Exception e) {
                    log.error("Can not init repository: \"{}\"", name, e);
                }
            }
            Closeables.closeIfCloseable(old);
        }
    }

    private void dispatchEvent(RegistryEvent event) {
        // we execute events in service only when event came from registry
        this.executorService.execute(() -> {
            this.eventBus.accept(event);
        });
    }

    public void unRegister(String name) {
        RegistryService registryService = registryServiceMap.remove(name);
        Assert.notNull(registryService, "registryService must not null");
        Closeables.closeIfCloseable(registryService);
        classMapper.delete(name);
        fireRegistryRemovedEvent(registryService.getConfig());
    }

    private void fireRegistryAddedEvent(RegistryConfig config) {
        RegistryEvent.Builder logEvent = new RegistryEvent.Builder();
        logEvent.setAction(StandardActions.CREATE);
        logEvent.setName(config.getName());
        if (StringUtils.hasText(config.getErrorMessage())) {
            logEvent.setSeverity(Severity.ERROR);
            logEvent.setMessage(config.getErrorMessage());
        } else {
            logEvent.setSeverity(Severity.INFO);
        }
        eventBus.accept(logEvent.build());
    }

    private void fireRegistryRemovedEvent(RegistryConfig config) {
        RegistryEvent.Builder logEvent = new RegistryEvent.Builder();
        logEvent.setAction(StandardActions.DELETE);
        logEvent.setName(config.getName());
        logEvent.setSeverity(Severity.INFO);
        eventBus.accept(logEvent.build());
    }

    public List<ImageCatalog> getCatalog(Collection<String> names) {

        if (CollectionUtils.isEmpty(names)) {
            names = getAvailableRegistries();
        }
        List<ImageCatalog> collect = names.stream()
                .filter(f -> !getRegistry(f).getConfig().isDisabled())
                .filter(f -> !StringUtils.hasText(getRegistry(f).getConfig().getErrorMessage()))
                .filter(f -> !(getRegistry(f) instanceof DockerHubRegistry))
                .map(n -> {
            ImageCatalog imageCatalog = getRegistry(n).getCatalog();
            imageCatalog.setName(n);
            return imageCatalog;
        }).collect(Collectors.toList());
        return collect;
    }

    public List<String> getTags(String name, String registryName, Filter filter) {
        RegistryService registry = getRegistry(registryName);
        Tags tags = registry.getTags(name);
        List<String> filtered = filter(tags, name, registry, filter);
        return filtered;
    }

    public ImageDescriptor getImage(String name, String tag, String registryName) {
        RegistryService registry = getRegistry(registryName);
        ImageDescriptor image = registry.getImage(name, tag);
        return image;
    }

    public boolean checkHealth(String registryName) {
        RegistryService registry = getRegistry(registryName);
        return registry.checkHealth();
    }

    private List<String> filter(Tags tags, String name, RegistryService registry, Filter filterSet) {
        if (tags == null) {
            return Collections.emptyList();
        }
        ImageFilterContext ifc = new ImageFilterContext(registry);
        ifc.setName(name);
        List<String> list = new ArrayList<>();
        for (String tag : tags.getTags()) {
            ifc.setTag(tag);
            boolean test = filterSet.test(ifc);
            if (test) {
                list.add(tag);
            }
        }
        return list;
    }

    public Collection<String> getAvailableRegistries() {
        return ImmutableSet.copyOf(registryServiceMap.keySet());
    }

    public void deleteTag(String name, String reference, String registryName) {
        RegistryService registry = getRegistry(registryName);
        registry.deleteTag(name, reference);

    }

    public RegistryConfig getConfig(String registry) {
        return getRegistry(registry).getConfig();
    }

    /**
     * Return registry or wrapper, never return null.
     * @param registry
     * @return
     */
    public RegistryService getRegistry(String registry) {
        if (!StringUtils.hasText(registry)) {
            return wrapDefault(registry);
        }
        RegistryService registryService = MoreObjects.firstNonNull(getByName(registry), wrapDefault(registry));

        if (registryService.getConfig().isDisabled() || StringUtils.hasText(registryService.getConfig().getErrorMessage())) {
            return new DisabledRegistryServiceWrapper(registryService);
        }
        return registryService;
    }

    private RegistryService wrapDefault(String registry) {
        return new DockerHubRegistryServiceWrapper(defaultRegistry, registry);
    }

    public RegistryService getDefaultRegistry() {
        return defaultRegistry;
    }

    /**
     * Get registry by name
     * @param registryName
     * @return
     */
    public RegistryService getByName(String registryName) {
        RegistryService service = registryServiceMap.get(registryName);
        if(service == null && Objects.equals(defaultRegistry.getConfig().getName(), registryName)) {
            service = defaultRegistry;
        }
        return service;
    }

    @Override
    public SearchResult search(String query, final int page, final int size) {
        RegistrySearchHelper rsh = new RegistrySearchHelper(query, page, size);
        for(RegistryService service: registryServiceMap.values()) {
            rsh.search(service);
        }
        rsh.search(defaultRegistry);
        return rsh.collect();
    }

    @ReConfigObject
    private RegistriesConfig getConfig() {
        List<RegistryConfig> configs = registryServiceMap.entrySet().stream().map(e -> {
            RegistryConfig config = e.getValue().getConfig();
            RegistryConfig exported = config.clone();
            exported.setName(e.getKey());
            return exported;
        }).collect(Collectors.toList());
        RegistriesConfig rsc = new RegistriesConfig();
        rsc.setRegistries(configs);
        return rsc;
    }

    @ReConfigObject
    private void setConfig(RegistriesConfig rc) {
        List<RegistryConfig> registries = rc.getRegistries();
        for(RegistryConfig config: registries) {
            RegistryService registryService = factory.createRegistryService(config);
            register(registryService);
        }

    }
}
