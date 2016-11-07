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
        return Manifests.read("dm-cluman-info-name");
    }

    /**
     * extract '$version' from manifest (Implementation-Version) or other places.
     *
     * @return
     */
    public static String getApplicationVersion() {
        return Manifests.read("dm-cluman-info-version");
    }

    public static OffsetDateTime getBuildTime() {
        return OffsetDateTime.parse(Manifests.read("dm-cluman-info-date"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

}
