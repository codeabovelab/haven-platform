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

package com.codeabovelab.dm.cluman.cluster.docker.model;

import com.codeabovelab.dm.common.json.JtEnumLower;
import com.codeabovelab.dm.common.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Arrays;
import java.util.Map;

/**
 * Mount represents a mount (volume). <p/>
 * https://github.com/docker/docker/blob/master/api/types/mount/mount.go
 */
@Data
@AllArgsConstructor
@Builder(builderClassName = "Builder")
public class Mount {

    @JsonProperty("Type")
    private final Type type;
    /**
     * Source specifies the name of the mount. Depending on mount type, this
     * may be a volume name or a host path, or even ignored.
     * Source is not supported for tmpfs (must be an empty value)
     */
    @JsonProperty("Source")
    private final String source;
    @JsonProperty("Target")
    private final String target;
    @JsonProperty("ReadOnly")
    private final boolean readonly;

    @JsonProperty("BindOptions")
    private final BindOptions bindOptions;
    @JsonProperty("VolumeOptions")
    private final VolumeOptions volumeOptions;
    @JsonProperty("TmpfsOptions")
    private final TmpfsOptions tmpfsOptions;

    /**
     * system mapping like:
     * "Type": "volume",
     * "Name": "810a2b9f8d08ba620612e1227d1214",
     * "Source": "/var/lib/docker/volumes/810a2b9f8d08ba620612e1/_data",
     * "Driver": "local",
     */
    public boolean isSystem() {
        return Arrays.stream(getSource().split("/")).anyMatch(s -> s.trim().length() == 64 && StringUtils.matchHex(s));
    }

    @JtEnumLower
    public enum Type {
        /**
         * the type for mounting host dir
         */
        BIND,
        /**
         * the type for remote storage volumes
         */
        VOLUME,
        /**
         * the type for mounting tmpfs
         */
        TMPFS
    }

    /**
     * Propagation represents the propagation of a mount.
     */
    @JtEnumLower
    public enum Propagation {
        RPRIVATE,
        PRIVATE,
        RSHARED,
        SHARED,
        RSLAVE,
        SLAVE
    }

    /**
     * BindOptions defines options specific to mounts of type "bind".
     */
    @Data
    @AllArgsConstructor
    @lombok.Builder(builderClassName = "Builder")
    public static class BindOptions {
        @JsonProperty("Propagation")
        private final Propagation propagation;
    }

    /**
     * VolumeOptions represents the options for a mount of type volume.
     */
    @Data
    @AllArgsConstructor
    @lombok.Builder(builderClassName = "Builder")
    public static class VolumeOptions {
        @JsonProperty("NoCopy")
        private final boolean noCopy;
        @JsonProperty("Labels")
        private final Map<String, String> labels;
        @JsonProperty("DriverConfig")
        private final Driver driverConfig;
    }

    /**
     * Driver represents a volume driver.
     */
    @Data
    @AllArgsConstructor
    @lombok.Builder(builderClassName = "Builder")
    public static class Driver {
        private static final String LOCAL = "local";

        @JsonProperty("Name")
        private final String name;
        @JsonProperty("Options")
        private final Map<String, String> options;

        public boolean isLocal() {
            return LOCAL.equals(name);
        }
    }

    /**
     * TmpfsOptions defines options specific to mounts of type "tmpfs".
     */
    @Data
    @AllArgsConstructor
    @lombok.Builder(builderClassName = "Builder")
    public static class TmpfsOptions {
        /**
         * Size sets the size of the tmpfs, in bytes.
         */
        @JsonProperty("SizeBytes")
        private final long size;
        /**
         * Unix mode (permissions) of the tmpfs upon creation
         */
        @JsonProperty("Mode")
        private final int mode;
    }
}
