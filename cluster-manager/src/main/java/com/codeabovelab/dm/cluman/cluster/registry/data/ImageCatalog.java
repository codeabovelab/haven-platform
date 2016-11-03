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

package com.codeabovelab.dm.cluman.cluster.registry.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 *
 * List of images available in the registry.
 */
public class ImageCatalog {

    private static final String REPOSITORIES = "repositories"; // images available in the registry

    private final List<String> images;
    private String name;

    public ImageCatalog(@JsonProperty(REPOSITORIES) List<String> repositories) {
        this.images = repositories;
    }

    @JsonProperty(REPOSITORIES)
    public List<String> getImages() {
        return images;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ImageCatalog{" +
                "images=" + images +
                '}';
    }
}
