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

package com.codeabovelab.dm.cluman;

import com.codeabovelab.dm.cluman.model.ContainerBaseIface;
import com.codeabovelab.dm.cluman.model.ImageName;
import com.codeabovelab.dm.common.utils.ContainerDetector;
import com.codeabovelab.dm.common.utils.OSUtils;
import org.springframework.util.StringUtils;

import java.util.*;

public final class ContainerUtils {

    private static final String SHA256 = "sha256:";

    public static boolean isImageId(String image) {
        // see https://docs.docker.com/registry/spec/api/#/content-digests
        int length = SHA256.length();
        return image.regionMatches(true, 0, SHA256, 0, length);
    }

    public static void assertImageName(String image) {
        if(!StringUtils.hasText(image)) {
            throw new IllegalArgumentException("Image name is null or empty");
        }
        if(isImageId(image)) {
            throw new IllegalArgumentException("is image id, but we expect name");
        }
    }

    /**
     * Returns application name for discovery-service by image name
     * example: example.com/com.example.core:172 -> com.example.core
     *
     * @param appName
     * @return
     */
    public static String getApplicationName(String appName) {
        // slash in name is not allowed, because it used in uri
        // also, internally  app name in upper case
        String imageName = getImageName(appName);
        int start = imageName.lastIndexOf('.') + 1;
        if (start < 0) {
            start = 0;
        }
        imageName = imageName.substring(start);
        return imageName.toUpperCase();
    }

    /**
     * Returns image name without Registry
     * example: example.com/com.example.core:172 -> com.example.core:172
     *
     * @param imageName
     * @return
     */
    public static String getImageVersionName(String imageName) {
        assertImageName(imageName);
        int start = imageName.lastIndexOf('/') + 1;
        if (start < 0) {
            start = 0;
        }
        return imageName.substring(start);
    }

    /**
     * Returns Registry url
     * example: example.com/com.example.core:172 -> example.com
     *
     * @param imageName
     * @return name or empty string when image does not have registry part
     */
    public static String getRegistryName(String imageName) {
        assertImageName(imageName);
        int end = imageName.lastIndexOf('/');
        if (end < 0) {
            return "";
        }
        String registry = imageName.substring(0, end);
        if (ImageName.isRegistry(registry)) {
            return registry;
        }
        return "";
    }

    /**
     * Returns image name without Registry name and version
     * example: example.com/com.example.core:172 -> com.example.core
     *
     * @param imageName
     * @return
     */
    public static String getImageName(String imageName) {
        String name = getImageVersionName(imageName);
        int lastIndex = name.lastIndexOf(':');
        if (lastIndex < 0) {
            lastIndex = name.length();
        }
        return name.substring(0, lastIndex);
    }

    /**
     * Return 'registry/image' name without version
     * example: example.com/com.example.core:172 -> example.com/com.example.core
     *
     * @param name
     * @return
     */
    public static String getRegistryAndImageName(String name) {
        assertImageName(name);
        int tagStart = name.lastIndexOf(':');
        int regEnd = name.indexOf('/');
        // we check that ':' is not part or registry name
        if (tagStart < 0 || tagStart <= regEnd) {
            tagStart = name.length();
        }
        return name.substring(0, tagStart);
    }

    /**
     * Returns image version
     * example: example.com/com.example.core:172 -> 172
     *
     * @param appName
     * @return null when no version, or version string
     */
    public static String getImageVersion(String appName) {
        assertImageName(appName);
        int start = appName.lastIndexOf(':');
        if (start < 0) {
            return null;
        }
        appName = appName.substring(start + 1);
        return appName;
    }

    /**
     * Make full image name from components
     * @param registry
     * @param image
     * @param tag
     * @return
     */
    public static String buildImageName(String registry, String image, String tag) {
        String regAndName = registry + "/" + image;
        if(tag == null) {
            return regAndName;
        }
        return setImageVersion(regAndName, tag);
    }

    /**
     * Change version part of image string. If 'version' is empty or null then do nothing.
     *
     * @param image
     * @param version
     * @return
     */
    public static String setImageVersion(String image, String version) {
        assertImageName(image);
        if (!StringUtils.hasText(version)) {
            return image;
        }
        int i = image.lastIndexOf(':');
        if (i < 0) {
            return image + ":" + version;
        }
        return image.substring(0, i + 1) + version;
    }

    public static Long parseMemorySettings(String memory) {
        if (!StringUtils.hasText(memory)) {
            return null;
        }
        String trim = memory.trim();
        if (isInteger(trim)) {
            return Long.parseLong(trim);
        } else {
            String substring = trim.substring(0, trim.length() - 1);
            if (isInteger(substring)) {
                long value = Long.parseLong(substring);
                String suffix = trim.substring(trim.length() - 1, trim.length());
                switch (suffix.toLowerCase()) {
                    case "k":
                        return value * 1024;
                    case "m":
                        return value * 1024 * 1024;
                    case "g":
                        return value * 1024 * 1024 * 1024;
                }
            } else {
                throw new IllegalArgumentException("can't parse memory settings: " + memory);
            }
        }

        return null;
    }

    public static boolean isInteger(String s) {
        Scanner sc = new Scanner(s.trim());
        if (!sc.hasNextInt()) return false;
        // we know it starts with a valid int, now make sure
        // there's nothing left!
        sc.nextInt();
        return !sc.hasNext();
    }

    public static int getPort(String addr) {
        if (addr == null) {
            return -1;
        }
        int portStart = addr.lastIndexOf(':');
        String portStr = addr.substring(portStart + 1);
        return Integer.parseInt(portStr);
    }

    public static String getHost(String addr) {
        if (addr == null) {
            return null;
        }
        int portStart = addr.lastIndexOf(':');
        return addr.substring(0, portStart);
    }


    public static boolean isOurContainer(ContainerBaseIface cont) {
        if (!ContainerDetector.isContainer()) {
            // we not in container and do not need to protect our container
            return false;
        }
        // depend of some options container can have hostname equal with name or part of container id
        String hostName = OSUtils.getHostName();
        String id = ContainerDetector.getId();
        String pcid = cont.getId();
        return cont.getName().equals(hostName) || (id == null ? pcid.startsWith(hostName) : pcid.equals(id));
    }

    public static String fetchTagSuffix(String imageVersion) {
        if (imageVersion == null) {
            return null;
        }
        int i = imageVersion.lastIndexOf("-");
        if (i <= 0) {
            return null;
        }
        return imageVersion.substring(i, imageVersion.length());
    }

    public static String fixContainerName(String name) {
        if(name.startsWith("/")) {
            // yes, there may appear other slashes,
            // but we can do not known this cases and ignore its
            name = name.substring(1);
        }
        return name;
    }
}
