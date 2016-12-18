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

package com.codeabovelab.dm.common.utils;

import com.jcabi.manifests.Manifests;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Tool for gathering application info
 */
public class AppInfo {

    /**
     * extract '$artifactId' from manifest (Implementation-Title) or other places.
     *
     * @return
     */
    public static String getApplicationName() {
        return getValue("dm-cluman-info-name");
    }

    /**
     * extract '$version' from manifest (Implementation-Version) or other places.
     *
     * @return
     */
    public static String getApplicationVersion() {
        return getValue("dm-cluman-info-version");
    }

    public static String getBuildRevision() {
        return getValue("dm-cluman-info-buildRevision");
    }

    public static OffsetDateTime getBuildTime() {
        try {
            return OffsetDateTime.parse(getValue("dm-cluman-info-date"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e) {
            return OffsetDateTime.now();
        }
    }

    private static String getValue(String key) {
        try {
            // we expect error like IllegalArgumentException: Attribute 'dm-cluman-info-version' not found in MANIFEST.MF file(s) among 90 other attribute(s):
            // which appear anytime when we run app without jar file
            return Manifests.read(key);
        } catch (IllegalArgumentException e) {
            return "MANIFEST_WAS_NOT_FOUND";
        }
    }


}
