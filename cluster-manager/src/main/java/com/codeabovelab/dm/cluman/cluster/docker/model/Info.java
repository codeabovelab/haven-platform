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

package com.codeabovelab.dm.cluman.cluster.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Info {

    private static final String ID = "Id";
    private static final String CONTAINERS = "Containers";
    private static final String IMAGES = "Images";
    private static final String NCPU = "NCPU";
    private static final String MEM_TOTAL = "MemTotal";
    private static final String DRIVER_STATUS = "DriverStatus";
    private static final String SYSTEM_STATUS = "SystemStatus";
    private static final String NAME = "Name";


    private final String id;
    private final Integer containers;
    private final Integer images;
    private final Integer ncpu;
    private final Long memory;
    private final String name;

    private final List<List<String>> driverStatus;
    private final List<List<String>> systemStatus;


    public Info(@JsonProperty(ID) String id,
                @JsonProperty(CONTAINERS) Integer containers,
                @JsonProperty(IMAGES) Integer images,
                @JsonProperty(NCPU) Integer ncpu,
                @JsonProperty(MEM_TOTAL) Long memory,
                @JsonProperty(NAME) String name,
                @JsonProperty(DRIVER_STATUS) List<List<String>> driverStatus,
                @JsonProperty(SYSTEM_STATUS) List<List<String>> systemStatus) {
        this.id = id;
        this.containers = containers;
        this.images = images;
        this.ncpu = ncpu;
        this.memory = memory;
        this.name = name;
        this.driverStatus = driverStatus;
        this.systemStatus = systemStatus;
    }

    @JsonProperty(ID)
    public String getId() {
        return id;
    }

    @JsonProperty(CONTAINERS)
    public Integer getContainers() {
        return containers;
    }

    @JsonProperty(IMAGES)
    public Integer getImages() {
        return images;
    }

    @JsonProperty(NCPU)
    public Integer getNcpu() {
        return ncpu;
    }

    @JsonProperty(MEM_TOTAL)
    public Long getMemory() {
        return memory;
    }

    @JsonProperty(DRIVER_STATUS)
    public List<List<String>> getDriverStatus() {
        return driverStatus;
    }

    @JsonProperty(SYSTEM_STATUS)
    public List<List<String>> getSystemStatus() {
        return systemStatus;
    }

    @JsonProperty(NAME)
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Info{" +
                "id='" + id + '\'' +
                ", containers=" + containers +
                ", images=" + images +
                ", ncpu=" + ncpu +
                ", memory=" + memory +
                ", name='" + name + '\'' +
                ", driverStatus=" + driverStatus +
                ", systemStatus=" + systemStatus +
                '}';
    }
}
