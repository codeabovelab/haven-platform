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

package com.codeabovelab.dm.common.format;

/**
 * Indicate that format of field is not valid
 *
 * return new ValidableFormatter<>(new PhoneFormatter(), null, new Callback<String>() {
 *   public void call(String arg) {
 *     if (!arg.matches("^[^\\d]*1[^\\d]*[38].*")) {
 *          throw new FormatterException("'" + arg + "' must start with 13 or 18");
 *      }
 *    }
 * });
 */
public class FormatterException extends RuntimeException {
    public FormatterException() {
    }

    public FormatterException(String message) {
        super(message);
    }

    public FormatterException(String message, Throwable cause) {
        super(message, cause);
    }

    public FormatterException(Throwable cause) {
        super(cause);
    }
}
