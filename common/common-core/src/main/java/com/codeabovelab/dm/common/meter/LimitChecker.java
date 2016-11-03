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

package com.codeabovelab.dm.common.meter;

/**
 * function which test State of meter and create error object if meters out of specified limit
 */
public interface LimitChecker {
    /**
     * test metric for exceeding of limit
     * @param context
     * @return null if metric stay in limits or LimitExcess instance otherwise
     */
    LimitExcess check(LimitCheckContext context);

    /**
     * period between checks. <p/>
     * now it value used one time, but in future we can implement support for dynamic period checking
     * @return
     */
    long getPeriod();
}
