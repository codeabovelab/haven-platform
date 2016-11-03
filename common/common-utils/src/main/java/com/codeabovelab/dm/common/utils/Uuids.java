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

import java.util.Random;
import java.util.UUID;

/**
 * Some uuid uitlities.
 */
public final class Uuids {

    private static final Random RANDOM = new Random();

    /**
     * Fast uuid validation. Accept only 36byte HEX with '-' value.
     * @param uuid
     * @return
     * @throws java.lang.IllegalArgumentException on invalid value
     */
    public static void validate(String uuid) throws IllegalArgumentException {
        if(uuid == null) {
            throw new IllegalArgumentException("Uuid is null.");
        }
        final int length = uuid.length();
        if(length != 36) {
            throw new IllegalArgumentException("Uuid: '" + uuid + "' - has invalid length.");
        }
        int i = 0;
        while(i < length) {
            char c = uuid.charAt(i);
            if(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f' ||
              (c == '-' && (i == 8 || i == 13 || i == 18 || i == 23))) {
                i++;
                continue;
            }
            throw new IllegalArgumentException("Uuid: '" + uuid + "' - has invalid char '" + c
                + "' at " + i + ".");
        }
    }

    /**
     * Create lite random uuid (it use non secure pseudo random generator).
     */
    public static UUID liteRandom() {
        long most = RANDOM.nextLong();
        long last = RANDOM.nextLong();
        return new UUID(most, last);
    }
}
