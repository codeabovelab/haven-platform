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

import com.codeabovelab.dm.cluman.cluster.registry.model.HubRegistryConfig;
import com.codeabovelab.dm.cluman.cluster.registry.model.RestTemplateFactory;

/**
 */
public class HubRegistryAdapter extends DockerRegistryAdapter<HubRegistryConfig> {

    private final String url;

    public HubRegistryAdapter(HubRegistryConfig config, RestTemplateFactory rtf, String url) {
        super(config, rtf);
        this.url = url;
    }

    @Override
    public String adaptNameForUrl(String name) {
        if(name.indexOf('/') < 0) {
            // it need because docker hub find simple names like 'nginx' under 'library/nginx' path
            return "library/" + name;
        }
        return name;
    }

    @Override
    public String getUrl() {
        return url;
    }
}
