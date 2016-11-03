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

/**
 * Simple implementation of server text messages.
 * @see ClientConnection#send(Object)
 */
public final class TextMessageImpl implements TextMessage {
    private final String title;
    private final String message;

    @JsonCreator
    public TextMessageImpl(@JsonProperty("title")   String title,
                           @JsonProperty("message") String message) {
        this.title = title;
        this.message = message;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TextMessageImpl)) {
            return false;
        }

        TextMessageImpl that = (TextMessageImpl) o;

        if (message != null ? !message.equals(that.message) : that.message != null) {
            return false;
        }
        if (title != null ? !title.equals(that.title) : that.title != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TextMessage{" +
          "title='" + title + '\'' +
          ", message='" + message + '\'' +
          '}';
    }
}
