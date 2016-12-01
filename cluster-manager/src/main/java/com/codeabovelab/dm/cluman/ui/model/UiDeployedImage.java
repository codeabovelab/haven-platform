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

package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.cluman.model.DockerContainer;
import com.codeabovelab.dm.cluman.model.ImageName;
import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UiDeployedImage extends UiImageData {

    @Data
    public static class UiContainerShort {
        private String name;
        private String id;
        private String node;

        public static UiContainerShort toUi(DockerContainer dc) {
            UiContainerShort uc = new UiContainerShort();
            uc.setId(dc.getId());
            uc.setName(dc.getName());
            uc.setNode(dc.getNode().getName());
            return uc;
        }
    }

    private final String name;
    private final String currentTag;
    private final String registry;
    private final List<UiContainerShort> containers = new ArrayList<>();

    public UiDeployedImage(DockerContainer dc, String registry) {
        super(dc.getImageId());
        ImageName in = ImageName.parse(dc.getImage());
        this.name = in.getName();
        this.currentTag = in.getTag();
        this.registry = registry;
    }

    public void addContainer(UiContainerShort uc) {
        containers.add(uc);
    }
}
