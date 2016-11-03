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

package com.codeabovelab.dm.common.security;

import org.springframework.security.acls.domain.AclFormattingUtils;
import org.springframework.security.acls.model.Permission;

/**
 */
public enum Action implements Permission {
    CREATE(2),
    READ(0),
    UPDATE(1),
    DELETE(3),
    //to DELETE we save order same BasePermission
    EXECUTE(4),
    /**
     * Permission to change internal structure or enclosing items
     */
    ALTER_INSIDE(5),
    ;

    private final int mask;
    private final char c;

    Action(int position) {
        this.mask = 1 << position;
        c = name().charAt(0);
    }

    /**
     * Letter that identity action
     * @return
     */
    public char getLetter() {
        return c;
    }

    @Override
    public int getMask() {
        return mask;
    }

    @Override
    public String getPattern() {
        return AclFormattingUtils.printBinary(mask, c);
    }

    public static Action fromLetter(char c) {
        for(Action action: values()) {
            if(action.getLetter() == c) {
                return action;
            }
        }
        return null;
    }
}
