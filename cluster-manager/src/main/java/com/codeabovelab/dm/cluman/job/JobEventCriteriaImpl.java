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

import lombok.Data;

/**
 * Criteria for event subscription
 */
@Data
public final class JobEventCriteriaImpl implements JobEventCriteria {

    private final String id;
    private final String type;

    public static boolean matcher(JobEventCriteria pattern, JobEventCriteria event) {
        if(pattern == null) {
            return true;
        }
        String patternId = pattern.getId();
        String patternType = pattern.getType();
        boolean matchId = patternId == null || patternId.equals(event.getId());
        boolean matchType = patternType == null || patternType.equals(event.getType());
        return matchId && matchType;
    }
}
