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

package com.codeabovelab.dm.cluman.utils;

import com.codeabovelab.dm.cluman.model.ContainerBaseIface;
import com.codeabovelab.dm.cluman.model.ImageName;
import com.codeabovelab.dm.common.utils.ContainerDetector;
import com.codeabovelab.dm.common.utils.OSUtils;


public final class ContainerUtils {

    /**
     * Due to some docker issues we can loose image name after pull new version of image with same tag.
     * So we store original image name in container labels.
     */
    public static final String LABEL_IMAGE_NAME = "com.codeabovelab.dm.image.name";

    /**
     * use {@link  ImageName#isId(String)}
     * @param image
     * @return
     */
    public static boolean isImageId(String image) {
        return ImageName.isId(image);
    }

    private static void assertImageName(String image) {
        ImageName.assertName(image);
    }

    /**
     * Returns application name for discovery-service by image name
     * example: example.com/com.example.core:172 -> com.example.core
     *
     * @param appName
     * @return calculated name
     */
    public static String getApplicationName(String appName) {
        // slash in name is not allowed, because it used in uri
        // also, internally  app name in upper case
        String imageName = getImageNameWithoutPrefix(appName);
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
     * @param imageName full image name
     * @return tag
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
     * Returns Registry prefix for dockerhub images and registry for private
     * example: example.com/com.example.core:172 -> example.com
     *
     * @param imageName full image name
     * @return name or empty string when image does not have registry part
     */
    public static String getRegistryPrefix(String imageName) {
        assertImageName(imageName);
        int end = imageName.lastIndexOf('/');
        if (end < 0) {
            return "";
        }
        return imageName.substring(0, end);
    }

    /**
     * Returns image name without Registry _prefix_ and version
     * example: example.com/com.example.core:172 -> com.example.core. <p/>
     * Due to docker naming contract we can not differ hub private repos from hub namespaces, therefore this method can
     * work incorrectly and now our registries accept image name with registry prefix.
     * @param imageName full image name
     * @return image name without any '/'
     */
    @Deprecated
    public static String getImageNameWithoutPrefix(String imageName) {
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
     * @see ImageName#withoutTag(String)
     * @param name
     * @return
     */
    public static String getRegistryAndImageName(String name) {
        return ImageName.withoutTag(name);
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
     *
     * @param registry
     * @param image
     * @param tag
     * @return
     */
    public static String buildImageName(String registry, String image, String tag) {
        String regAndName = registry + "/" + image;
        if (tag == null) {
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
        return ImageName.setTag(image, version);
    }

    public static boolean isOurContainer(ContainerBaseIface cont) {
        if (!ContainerDetector.isContainer()) {
            // haven was not started in container, skipping protection
            return false;
        }
        // depend of some options container can have hostname equal with name or part of container id
        String hostName = OSUtils.getHostName();
        String id = ContainerDetector.getId();
        String pcid = cont.getId();
        boolean sameName = hostName.equals(cont.getName());
        boolean sameId = id == null ? pcid.startsWith(hostName) : pcid.equals(id);
        return sameName || sameId;
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
        if (name.startsWith("/")) {
            // yes, there may appear other slashes,
            // but we can do not known this cases and ignore its
            name = name.substring(1);
        }
        return name;
    }

    public static boolean isContainerId(String id) {
        // id like f75dea595d92ae635125ba37300c076682e80a311149782707a8c43893582236
        if (id == null || id.length() != 64) {
            return false;
        }
        for (int i = 0; i < id.length(); ++i) {
            char c = id.charAt(i);
            if (!isHex(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHex(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    /**
     * In some cases docker give image id instead of image name.
     * @see #LABEL_IMAGE_NAME
     * @param container
     * @return
     */
    public static String getFixedImageName(ContainerBaseIface container) {
        String image = container.getImage();
        if(ImageName.isId(image)) {
            String imageName = container.getLabels().get(ContainerUtils.LABEL_IMAGE_NAME);
            if(imageName != null && !ImageName.isId(imageName)) {
                return imageName;
            }
        }
        return image;
    }
}
