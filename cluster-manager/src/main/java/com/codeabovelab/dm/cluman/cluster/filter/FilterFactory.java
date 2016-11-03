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

package com.codeabovelab.dm.cluman.cluster.filter;

import com.codeabovelab.dm.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 */
@Component
public class FilterFactory {

    public final static String ANY = "any:*";
    public final static String NO_ONE = "noOne:*";

    public interface Factory {
        Filter create(String expr);
        default String getProtocol() {
            return null;
        }
    }

    private final ConcurrentMap<String, Factory> factories = new ConcurrentHashMap<>();

    public FilterFactory() {
        registerFilter(expr -> Filter.any(), "any");
        registerFilter(expr -> Filter.noOne(), "noOne");
        registerFilter(ImageSpelFilter::new, ImageSpelFilter.PROTO);
        registerFilter(ListFilter::new, ListFilter.PROTO);
        registerFilter(LabelFilter::new, LabelFilter.PROTO);
        registerFilter(RegexFilter::new, RegexFilter.PROTO);
        registerFilter(PatternFilter::new, PatternFilter.PROTO);
        registerFilter(ClusterFilter::new, ClusterFilter.PROTO);
    }

    @Autowired(required = false)
    public void onFilterFactories(List<Factory> factories) {
        if(factories == null) {
            return;
        }
        factories.forEach(this::registerFilter);
    }

    public void registerFilter(Factory factory) {
        String protocol = factory.getProtocol();
        Assert.notNull(protocol, factory + " got invalid protocol.");
        registerFilter(factory, protocol);
    }

    public void registerFilter(Factory factory, String protocol) {
        factories.put(protocol, factory);
    }

    public Filter createFilter(String expr) {
        String proto = StringUtils.before(expr, ':');
        Factory ff = factories.get(proto);
        Assert.notNull(ff, "can not find factory for: " + expr);
        return ff.create(expr.substring(proto.length() + 1));
    }

}
