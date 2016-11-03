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

package com.codeabovelab.dm.cluman.model;

import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerConfig;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * Common iface for image descriptor
 */
public interface ImageDescriptor {
    /**
     * Id of image. Usually start with hash name prefix.
     * @return
     */
    String getId();
    Date getCreated();

    /**
     * map of image labels, usually it retrieved from container config
     * @return
     */
    default Map<String, String> getLabels() {
        ContainerConfig cc = getContainerConfig();
        Map<String, String> labels = cc == null? null : cc.getLabels();
        return labels == null? Collections.emptyMap() : Collections.unmodifiableMap(labels);
    }

    /**
     * Container config. Implementation may lazy load this data, and therefore it may consume some time.
     * @return
     */
    ContainerConfig getContainerConfig();
}
