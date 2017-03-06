/*
 * Copyright 2017 Code Above Lab LLC
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

import com.codeabovelab.dm.common.utils.Cloneables;
import com.codeabovelab.dm.common.utils.Comparables;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
@JsonPropertyOrder({"name", "image", "cluster", "application", "labels", "ports"})
@Data
@ToString(callSuper = true)
public class ServiceSource implements Cloneable, Comparable<ServiceSource> {
    private String id;
    private String name;
    private String image;
    private String imageId;
    /**
     * Name of swarm in which container will be created
     */
    private String cluster;
    /**
     * Application name
     */
    private String application;
    @Setter(AccessLevel.NONE)
    @JsonPropertyOrder(alphabetic = true)
    private List<Port> ports = new ArrayList<>();
    @Setter(AccessLevel.NONE)
    @JsonPropertyOrder(alphabetic = true)
    private Map<String, String> labels = new HashMap<>();
    @Setter(AccessLevel.NONE)
    @JsonPropertyOrder(alphabetic = true)
    private List<String> constraints = new ArrayList<>();
    @Setter(AccessLevel.NONE)
    private ContainerSource container = new ContainerSource();

    /**
     * Set clone of argument as container.
     * @param container  argument for cloning, can be null
     */
    public void setContainer(ContainerSource container) {
        this.container = Cloneables.clone(container);
    }

    @Override
    public ServiceSource clone() {
        ServiceSource clone;
        try {
            clone = (ServiceSource) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        clone.ports = Cloneables.clone(clone.ports);
        clone.labels = Cloneables.clone(clone.labels);
        clone.constraints = Cloneables.clone(clone.constraints);
        clone.container = Cloneables.clone(clone.container);
        return clone;
    }

    @Override
    public int compareTo(ServiceSource o) {
        return Comparables.compare(this.name, o.name);
    }
}
