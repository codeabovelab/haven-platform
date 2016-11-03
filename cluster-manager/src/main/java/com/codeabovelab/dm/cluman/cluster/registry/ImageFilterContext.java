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

import com.codeabovelab.dm.cluman.model.ImageDescriptor;
import com.codeabovelab.dm.cluman.model.Labels;
import com.codeabovelab.dm.cluman.model.Named;

import java.util.Map;

/**
 * Context for filtering one image
 */
public class ImageFilterContext implements Named, Labels {
    private final RegistryService registryService;
    private String name;
    private String tag;
    private ImageDescriptor image;

    public ImageFilterContext(RegistryService registryService) {
        this.registryService = registryService;
    }

    public void setName(String name) {
        this.name = name;
        reset();
    }


    public void setTag(String tag) {
        this.tag = tag;
        reset();
    }

    /**
     * Current registry service
     * @return
     */
    public RegistryService getRegistryService() {
        return registryService;
    }

    private void reset() {
        this.image = null;
    }

    /**
     * Retrieve labels. <b>This operation is time consuming.</b>
     * @return
     */
    public Map<String, String> getLabels() {
        return getImage().getContainerConfig().getLabels();
    }


    /**
     * Retrieve image description from service. <b>This operation is time consuming.</b>
     * @return
     */
    public ImageDescriptor getImage() {
        if(this.image == null) {
            this.image = registryService.getImage(name, tag);
        }
        return image;
    }

    /**
     * Name of current image
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Tag of current image
     * @return
     */
    public String getTag() {
        return tag;
    }
}
