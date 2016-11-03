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

import lombok.Data;

/**
 * Parsed representation of image name. <p/>
 * Note that all fields can not be null, when it not have any data then it present as empty string.
 */
@Data
public class ImageName {

    /**
     * Docker use below string constant as tag when can not find any tag or name for image.
     */
    public static final String NONE = "<none>";
    /**
     * Docker use below string constant as name when can not find any tag or name for image.
     */
    public static final String NONE_NAME = NONE + ":" + NONE;
    /**
     * Length of id which is used as name.
     */
    public static final int NAME_ID_LEN = 12;

    private final String registry;
    private final String name;
    private final String tag;

    public static ImageName parse(String src) {
        if(src == null) {
            return null;
        }
        final int len = src.length();
        int registryEnd = src.indexOf('/');
        int tagBegin = src.indexOf(':', registryEnd);
        if(tagBegin == -1) {
            tagBegin = len;
        }
        String registry = registryEnd < 0 ? "" : src.substring(0, registryEnd);
        if(!isRegistry(registry)) {
            registry = "";
            registryEnd = -1;
        }
        String name = src.substring(registryEnd + 1, tagBegin);
        String tag = (tagBegin < len)?  src.substring(tagBegin + 1) : "";
        return new ImageName(registry, name, tag);
    }

    /**
     * Docker allow namespaces like 'some/ubuntu' and we need to differ its from registry name
     * for that we <a href="https://github.com/docker/docker/blob/master/reference/reference.go#L176">use code from docker </a>
     * @param registry
     * @return true if it valid registry name
     */
    public static boolean isRegistry(String registry) {
        return "localhost".equals(registry) || registry.indexOf('.') > 0 || registry.indexOf(':') > 0;
    }

    /**
     * Get name of image. <p/>
     * Some images does not has any tag, then docker use {@link #NONE_NAME }, but docker client show first
     * 12 digits of imageId, we reproduce this behaviour.
     * @return
     */
    public static String getName(String name, String imageId) {
        if(name != null && !name.contains(ImageName.NONE)) {
            return name;
        }
        return nameFromId(imageId);
    }

    public static String nameFromId(String imageId) {
        int start = imageId.indexOf(':') + 1;
        return imageId.substring(start, start + ImageName.NAME_ID_LEN);
    }
}
