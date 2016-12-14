/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.cluman.batch;

import com.codeabovelab.dm.cluman.model.ImageName;
import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.springframework.util.PatternMatchUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
@Data
public class ImagesForUpdate {

    @Data
    public static class Builder {
        private final List<Image> images = new ArrayList<>();

        public Builder addImage(String name, String from, String to) {
            addImage(new Image(name, from, to));
            return this;
        }

        public Builder addImage(Image image) {
            this.images.add(image);
            return this;
        }

        public Builder images(List<Image> images) {
            setImages(images);
            return this;
        }

        public void setImages(List<Image> images) {
            this.images.clear();
            if(images != null) {
                this.images.addAll(images);
            }
        }

        public ImagesForUpdate build() {
            return new ImagesForUpdate(this);
        }
    }

    @Data
    public static class Image {
        @ApiModelProperty("Name or pattern (like 'registry/image*') of image with registry, but without tag. '*' match all images")
        private final String name;
        @ApiModelProperty("Comma delimited list of tag patterns from which image will be upgraded. " +
          "If null then system update all versions of this image.")
        private final String from;
        @ApiModelProperty("Destination tag to which image will be upgraded. If you leave " +
          "null value then system will upgrade to last version. Also you can specify pattern of tag, like '*-stable'")
        private final String to;

        /**
         * 'to' match all
         * @return true if 'to' match all
         */
        @JsonIgnore
        public boolean isAllTo() {
            return isAll(to);
        }

        /**
         * 'from' match all
         * @return true if 'from' match all
         */
        @JsonIgnore
        public boolean isAllFrom() {
            return isAll(from);
        }

        private static boolean isAll(String from) {
            return from == null || "*".equals(from);
        }

        public boolean matchTo(String image, String imageId) {
            return match(to, image, imageId);
        }

        public boolean matchFrom(String image, String imageId) {
            return match(from, image, imageId);
        }

        private static boolean match(String pattern, String image, String imageId) {
            if(isAll(pattern)) {
                return true;
            }
            boolean isPattern = isPattern(pattern);
            if(ImageName.isId(image) && !isPattern) {
                return pattern.equals(imageId == null? image : imageId);
            }
            String imageVersion = ContainerUtils.getImageVersion(image);
            if(isPattern) {
                return PatternMatchUtils.simpleMatch(pattern, imageVersion);
            } else {
                return pattern.equals(imageVersion);
            }
        }
    }

    private final List<Image> images;
    @Getter(AccessLevel.NONE)
    private final Map<String, Image> imagesByName = new HashMap<>();
    @Getter(AccessLevel.NONE)
    private final List<Image> imagesWithPattern = new ArrayList<>();

    @JsonCreator
    public ImagesForUpdate(Builder builder) {
        this.images = ImmutableList.copyOf(builder.images);
        this.images.forEach((img) -> {
            String name = img.getName();
            if(name.indexOf('*') < 0) {
                imagesByName.put(name, img);
            } else {
                imagesWithPattern.add(img);
            }
        });
    }

    public static Builder builder() {
        return new Builder();
    }

    public static boolean isPattern(String str) {
        return str != null && str.indexOf('*') >= 0;
    }

    /**
     * Find image by its name, or id
     * @param name
     * @param imageId
     * @return
     */
    public Image findImage(String name, String imageId) {
        Image res = null;
        if(imageId != null) {
            res = imagesByName.get(imageId);
        }
        String withoutTag = ImageName.withoutTagOrNull(name);
        if(res == null && withoutTag != null) {
            res = imagesByName.get(withoutTag);
        }
        if(res == null && (imageId != null || withoutTag != null)) {
            for(Image img: imagesWithPattern) {
                String pattern = img.getName();
                if(withoutTag != null && PatternMatchUtils.simpleMatch(pattern, withoutTag) ||
                   imageId != null && PatternMatchUtils.simpleMatch(pattern, imageId)) {
                    res = img;
                    break;
                }
            }
        }
        return res;
    }
}
