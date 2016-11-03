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

package com.codeabovelab.dm.common.validate;

import org.springframework.util.Assert;

import javax.validation.ConstraintViolation;
import java.util.Set;

/**
 */
public class JsrValidityImpl implements Validity {

    private final Set<? extends ConstraintViolation<?>> violations;
    private final String object;
    private volatile String message;
    private final Object lock = new Object();

    public JsrValidityImpl(String object, Set<? extends ConstraintViolation<?>> violations) {
        Assert.hasText(object, "object is null or empty");
        this.object = object;
        Assert.notNull(violations, "violations is null");
        this.violations = violations;
    }

    @Override
    public String getObject() {
        return this.object;
    }

    @Override
    public boolean isValid() {
        return violations.isEmpty();
    }

    @Override
    public String getMessage() {
        if(message == null) {
            synchronized (lock) {
                if(message == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("'").append(this.object).append("' properties: ");
                    boolean first = true;
                    for(ConstraintViolation<?> violation: violations) {
                        if(first) {
                            first = false;
                        } else {
                            sb.append(", ");
                        }
                        sb.append(violation.getPropertyPath().toString()).append(" - ");
                        sb.append(violation.getMessage());
                    }
                    message = sb.toString();
                }
            }
        }
        return message;
    }
}
