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

import com.codeabovelab.dm.cluman.model.ContainerSource;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.util.StringUtils;

/**
 * Represents a host path being bind mounted as a {@link VolumeRef} in a Docker container. The Bind can be in read only or
 * read write access mode.
 */
public class Bind {

    private String path;

    private VolumeRef volume;

    private AccessMode accessMode;

    public Bind(String path, VolumeRef volume) {
        this(path, volume, AccessMode.DEFAULT);
    }

    public Bind(String path, VolumeRef volume, AccessMode accessMode) {
        this.path = path;
        this.volume = volume;
        this.accessMode = accessMode;
    }

    public String getPath() {
        return path;
    }

    public VolumeRef getVolume() {
        return volume;
    }

    public AccessMode getAccessMode() {
        return accessMode;
    }

    /**
     * Parses a bind mount specification to a {@link Bind}.
     *
     * @param serialized
     *            the specification, e.g. <code>/host:/container:ro</code>
     * @return a {@link Bind} matching the specification
     * @throws IllegalArgumentException
     *             if the specification cannot be parsed
     */
    public static Bind parse(String serialized) {
        try {
            String[] parts = StringUtils.delimitedListToStringArray(serialized, ":");
            switch (parts.length) {
                case 2: {
                    return new Bind(parts[0], new VolumeRef(parts[1]));
                }
                case 3: {
                    AccessMode accessMode = AccessMode.valueOf(parts[2].toLowerCase());
                    return new Bind(parts[0], new VolumeRef(parts[1]), accessMode);
                }
                default: {
                    throw new IllegalArgumentException();
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing Bind '" + serialized + "'");
        }
    }

    /**
     * Parse value-path pairs give from {@link ContainerSource#getVolumeBinds()}.
     * @param volumeStr name or path to volume
     * @param pathStr path, with optionally AccessMode
     * @return
     */
    public static Bind parse(String volumeStr, String pathStr) {
        VolumeRef volume = new VolumeRef(volumeStr);
        String[] parts = StringUtils.split(pathStr, ":");
        if(parts == null) {
            return new Bind(pathStr, volume);
        }
        AccessMode accessMode = AccessMode.valueOf(parts[1].toLowerCase());
        return new Bind(parts[0], volume, accessMode);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bind) {
            Bind other = (Bind) obj;
            return new EqualsBuilder().append(path, other.getPath()).append(volume, other.getVolume())
                    .append(accessMode, other.getAccessMode()).isEquals();
        } else
            return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(path).append(volume).append(accessMode).toHashCode();
    }

    /**
     * Returns a string representation of this {@link Bind} suitable for inclusion in a JSON message. The format is
     * <code>&lt;host path&gt;:&lt;container path&gt;:&lt;access mode&gt;</code>, like the argument in
     * {@link #parse(String)}.
     *
     * @return a string representation of this {@link Bind}
     */
    @Override
    public String toString() {
        return path + ":" + volume.getPath() + ":" + accessMode.toString();
    }

}
