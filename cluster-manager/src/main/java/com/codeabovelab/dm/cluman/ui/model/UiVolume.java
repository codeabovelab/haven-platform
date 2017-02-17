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

package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.cluman.cluster.docker.model.Volume;
import com.codeabovelab.dm.common.utils.Sugar;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 */
@Data
public class UiVolume {
    private String name;
    private String driver;
    private final Map<String, String> options = new HashMap<>();
    private final Map<String, String> labels = new HashMap<>();
    @ApiModelProperty(readOnly = true)
    private String mountpoint;
    @ApiModelProperty(readOnly = true)
    private Volume.Scope scope;
    @ApiModelProperty(readOnly = true)
    private final Map<String, String> status = new HashMap<>();
    @ApiModelProperty(readOnly = true)
    private int usageRefCount;
    @ApiModelProperty(readOnly = true)
    private long usedSize;

    public static UiVolume from(Volume volume) {
        UiVolume ui = new UiVolume();
        ui.setName(volume.getName());
        ui.setDriver(volume.getDriver());
        Sugar.setIfNotNull(ui.getOptions()::putAll, volume.getOptions());
        Sugar.setIfNotNull(ui.getLabels()::putAll, volume.getLabels());
        ui.setMountpoint(volume.getMountpoint());
        ui.setScope(volume.getScope());
        Sugar.setIfNotNull(ui.getStatus()::putAll, volume.getStatus());
        Volume.VolumeUsageData ud = volume.getUsageData();
        if(ud != null) {
            ui.setUsageRefCount(ud.getRefCount());
            ui.setUsedSize(ud.getSize());
        }
        return ui;
    }
}
