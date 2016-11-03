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

package com.codeabovelab.dm.cluman.job;

import com.codeabovelab.dm.cluman.model.EventWithTime;
import com.codeabovelab.dm.common.json.StringConverter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 */
@Data
public class JobEvent implements JobEventCriteria, EventWithTime {
    public static final String BUS = "bus.cluman.job";
    private final LocalDateTime time = LocalDateTime.now();
    private final JobInfo info;
    private final String message;
    @JsonSerialize(converter = StringConverter.class)
    private final Throwable exception;

    @Override
    public long getTimeInMilliseconds() {
        return time.toEpochSecond(ZoneOffset.UTC);
    }

    @Override
    public String getId() {
        return info.getId();
    }

    @Override
    public String getType() {
        return info.getType();
    }
}
