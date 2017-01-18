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

package com.codeabovelab.dm.common.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 */
public final class TimeUtils {

    private TimeUtils() {
    }

    /**
     * Convert {@link LocalDateTime } to epoch milliseconds.
     * @param dt date time or null
     * @return milliseconds or Long.MIN_VALUE when argument is null
     */
    public static long toMillis(LocalDateTime dt) {
        if(dt == null) {
            return Long.MIN_VALUE;
        }
        return dt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
