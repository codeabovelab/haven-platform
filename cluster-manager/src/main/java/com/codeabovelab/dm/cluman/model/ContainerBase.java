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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableMap;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Base for any docker container
 */
@Data
public class ContainerBase implements ContainerBaseIface {

    @Data
    public static class Builder implements ContainerBaseIface {
        protected String id;
        protected String name;
        protected String image;
        protected String imageId;
        protected final Map<String, String> labels = new HashMap<>();

        public void setLabels(Map<String, String> labels) {
            this.labels.clear();
            if(labels != null) {
                this.labels.putAll(labels);
            }
        }

        @SuppressWarnings("unchecked")
        public ContainerBase build() {
            return new ContainerBase(this);
        }

        public Builder from(ContainerBaseIface c) {
            setId(c.getId());
            setName(c.getName());
            setImage(c.getImage());
            setImageId(c.getImageId());
            setLabels(c.getLabels());
            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }


    public static ContainerBase of(ContainerBaseIface c) {
        ContainerBase dc;
        if(c instanceof ContainerBase) {
            dc = (ContainerBase) c;
        } else {
            ContainerBase.Builder b = ContainerBase.builder();
            b.from(c);
            dc = b.build();
        }
        return dc;
    }


    private final String id;
    private final String name;
    private final String image;
    private final String imageId;
    private final Map<String, String> labels;

    @JsonCreator
    public ContainerBase(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.image = b.image;
        this.imageId = b.imageId;
        this.labels = ImmutableMap.copyOf(b.labels);
    }

}
