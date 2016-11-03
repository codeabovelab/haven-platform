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

package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.cluman.ui.HttpException;
import com.codeabovelab.dm.common.utils.Throwables;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DTO which is used for ui errors
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UiError extends UIResult {
    /**
     * Stack trace of error
     */
    private String stack;

    /**
     * Utility for creating error dto from exception.
     * Also support {@link HttpException }
     * @param ex
     * @return
     */
    public static UiError from(Exception ex) {
        UiError error = new UiError();
        error.setStack(Throwables.printToString(ex));
        if(ex instanceof HttpException) {
            error.setCode(((HttpException)ex).getStatus().value());
        }
        error.setMessage(ex.getMessage());
        return error;
    }

    @Override
    public String toString() {
        return "UiError{" +
                "stack='" + stack + '\'' +
                "} " + super.toString();
    }
}
