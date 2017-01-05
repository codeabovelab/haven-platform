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

package com.codeabovelab.dm.common.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Simple implementation of server text messages.
 * @see ClientConnection#send(Object)
 */
@Data
public final class TextMessageImpl implements TextMessage {
    private final String title;
    private final String message;

    @JsonCreator
    public TextMessageImpl(@JsonProperty("title")   String title,
                           @JsonProperty("message") String message) {
        this.title = title;
        this.message = message;
    }

}
