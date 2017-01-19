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
import org.springframework.util.StringUtils;

/**
 * Parsed representation of image name. <p/>
 * Note that all fields can not be null, when it not have any data then it present as empty string.
 */
@Data
public class ImageName {

    private static final String SHA256 = "sha256:";
    public static final String TAG_LATEST = "latest";
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
    private final String fullName;
    private final String id;

    // do not publish this constructor
    ImageName(String registry, String name, String tag) {
        this.registry = registry;
        this.name = name;
        this.tag = tag;
        this.id = null;
        this.fullName = toFullName(registry, name, tag);
    }

    private ImageName(String src, String id) {
        this.fullName = src;
        this.id = id;
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
        this.registry = registry;
        this.name = src.substring(registryEnd + 1, tagBegin);
        this.tag = (tagBegin < len)?  src.substring(tagBegin + 1) : "";
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getTag() {
        return tag;
    }

    public String getRegistry() {
        return registry;
    }

    public String getName() {
        return name;
    }

    private static String toFullName(String registry, String name, String tag) {
        return registry + '/' + name + ':' + tag;
    }

    public static ImageName parse(String src) {
        if(src == null) {
            return null;
        }
        int at = src.indexOf('@');
        if(at < 0) {
            return new ImageName(src, null);
        }
        return new ImageName(src.substring(0, at), src.substring(at + 1));
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

    public static boolean isId(String image) {
        // see https://docs.docker.com/registry/spec/api/#/content-digests
        int length = SHA256.length();
        if(image.regionMatches(true, 0, SHA256, 0, length)) {
            return true;
        }
        // sometime image name is created from id,
        // usual it has ImageName.NAME_ID_LEN first symbols from id
        return com.codeabovelab.dm.common.utils.StringUtils.matchHex(image);
    }

    /**
     * Check that argument is not empty and valid image name (not an image id)
     * @see #isId(String)
     * @param image
     */
    public static void assertName(String image) {
        if (!StringUtils.hasText(image)) {
            throw new IllegalArgumentException("Image name is null or empty");
        }
        if (isId(image)) {
            throw new IllegalArgumentException(image + " is image id, but we expect name");
        }
    }

    /**
     * Return 'registry/image' name without version
     * example: example.com/com.example.core:172 -> example.com/com.example.core
     * @param name
     * @return name without tag or throw exception
     */
    public static String withoutTag(String name) {
        assertName(name);
        return removeTagP(name);
    }

    private static String removeTagP(String name) {
        int tagStart = name.lastIndexOf(':');
        int regEnd = name.indexOf('/');
        // we check that ':' is not part or registry name
        if (tagStart < 0 || tagStart <= regEnd) {
            tagStart = name.length();
        }
        return name.substring(0, tagStart);
    }

    /**
     * Return 'registry/image' name without version
     * example: example.com/com.example.core:172 -> example.com/com.example.core
     * @param name
     * @return name without tag or null
     */
    public static String withoutTagOrNull(String name) {
        if(!StringUtils.hasText(name) || isId(name)) {
            return null;
        }
        return removeTagP(name);
    }
}
