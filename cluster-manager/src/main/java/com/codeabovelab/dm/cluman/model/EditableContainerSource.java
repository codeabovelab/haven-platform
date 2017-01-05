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

import com.codeabovelab.dm.common.json.JtMemory;
import lombok.Data;

/**
 * Part of ContainerSource which has updatable properties.
 */
@Data
public class EditableContainerSource implements Cloneable {


    private Integer cpuShares;
    private Integer cpuQuota;
    /**
     * By default, all containers get the same proportion of block IO bandwidth (blkioWeight). This proportion is 500.
     * To modify this proportion, change the container’s blkioWeight weight relative to the weighting of all other running containers.
     * The flag can set the weighting to a value between 10 to 1000.
     */
    private Integer blkioWeight;

    private Integer cpuPeriod;
    /**
     * Set cpu numbers in which to allow execution for containers. <p/>
     * <code>"1,3"</code> - executed on cpu 1 and cpu 3 <p/>
     * <code>"0-2"</code> - executed on cpu 0, cpu 1 and cpu 2. <p/>
     */
    private String cpusetCpus;
    /**
     * Set cpu numbers in which to allow execution for containers. <p/>
     * Memory nodes (MEMs) in which to allow execution (0-3, 0,1). Only effective on NUMA systems.
     * <code>"1,3"</code> - executed on cpu 1 and cpu 3 <p/>
     * <code>"0-2"</code> - executed on cpu 0, cpu 1 and cpu 2. <p/>
     */
    private String cpusetMems;
    /**
     * Restart policy to apply when a container exits (default 'no') <p/>
     * Possible values are : 'no', 'on-failure'[':'max-retry], 'always', 'unless-stopped'
     */
    private String restart;

    @JtMemory
    private Long memoryLimit;
    @JtMemory
    private Long memorySwap;
    @JtMemory
    private Long memoryReservation;
    @JtMemory
    private Long kernelMemory;

    @Override
    public EditableContainerSource clone() {
        try {
            return (EditableContainerSource) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
