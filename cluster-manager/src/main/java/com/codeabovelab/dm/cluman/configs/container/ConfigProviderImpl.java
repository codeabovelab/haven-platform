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

package com.codeabovelab.dm.cluman.configs.container;

import com.codeabovelab.dm.cluman.model.ContainerSource;
import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import com.codeabovelab.dm.common.utils.pojo.PojoUtils;
import com.codeabovelab.dm.common.utils.pojo.Property;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * Resolves properties from multiple sources and merge to one CreateContainerArg
 */
@Slf4j
@Component
public class ConfigProviderImpl implements ConfigProvider {

    private final List<ConfigsFetcher> fetcherList;

    @Autowired
    public ConfigProviderImpl(List<ConfigsFetcher> fetchers) {
        fetcherList = fetchers;
        fetcherList.sort(AnnotationAwareOrderComparator.INSTANCE);
    }

    public ContainerSource resolveProperties(String cluster, ImageDescriptor image, String imageName, ContainerSource original) {

        ContainerCreationContext context = ContainerCreationContext.builder().cluster(cluster).image(image)
                .imageName(imageName).containerName(Optional.ofNullable(original.getName())).build();
        for (ConfigsFetcher configsFetcher : fetcherList) {
            try {
                configsFetcher.resolveProperties(context);
            } catch (Exception e) {
                log.error("can't process config for image " + image, e);
            }
        }
        List<ContainerSource> configs = context.getArgList();
        configs.add(original);
        ContainerSource result = new ContainerSource();
        Map<String, Property> load = PojoUtils.load(ContainerSource.class);
        for (ContainerSource srcConfig : configs) {
            forConfig(result, load, srcConfig);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void forConfig(ContainerSource dest, Map<String, Property> props, ContainerSource srcConfig) {
        for (Property prop : props.values()) {
            try {
                Object o = prop.get(srcConfig);
                if (o == null) {
                    continue;
                }
                if (prop.isWritable()) {
                    Class<?> type = prop.getType();
                    if(Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
                        // we must not set collections, just add elements to them
                        log.warn("Ignore modifiable property of collection type: {}.{}", prop.getDeclaringClass(), prop.getName());
                        continue;
                    }
                }
                // we try to accumulate value for non null collection
                // note that set collections is bad way because it may be shared between objects and it
                // may cause difficult localised errors
                if (o instanceof Collection) {
                    Object r = prop.get(dest);
                    if (r != null && r instanceof Collection) {
                        Collection<Object> destCol = (Collection<Object>) r;
                        Collection<Object> coll = new LinkedHashSet<>((Collection<Object>) o);
                        coll.removeAll(destCol);
                        destCol.addAll(coll);
                        continue;
                    }
                }
                if (o instanceof Map) {
                    Object r = prop.get(dest);
                    if (r != null && r instanceof Map) {
                        Map<Object, Object> destMap = (Map<Object, Object>) r;
                        destMap.putAll((Map<Object, Object>) o);
                        continue;
                    }
                }
                if (prop.isWritable()) {
                    prop.set(dest, o);
                }
            } catch (Exception e) {
                log.error("Can't process property: " + prop.getName(), e);
            }

        }
    }


}
