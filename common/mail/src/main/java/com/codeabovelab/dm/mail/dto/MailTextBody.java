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

package com.codeabovelab.dm.mail.dto;

import org.springframework.util.MimeType;

import java.beans.Transient;
import java.io.Reader;
import java.io.StringReader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN;

/**
 */
public class MailTextBody implements MailBody {

    private String text;
    private final MimeType mimeType;

    public MailTextBody() {
        this.mimeType = new MimeType(TEXT_PLAIN.getType(), TEXT_PLAIN.getSubtype(), UTF_8);
    }

    public MailTextBody(String text) {
        this.text = text;
        this.mimeType = new MimeType(TEXT_PLAIN.getType(), TEXT_PLAIN.getSubtype(), UTF_8);
    }

    public MailTextBody(String text, MimeType mimeType) {
        this.text = text;
        this.mimeType = mimeType;
    }

    @Override
    public MimeType getMimeType() {
        return mimeType;
    }

    @Override
    @Transient
    public Reader getReader() {
        return new StringReader(text);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "TextMailBody{" +
          "text='" + text + '\'' +
          ", mimeType=" + mimeType +
          '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MailTextBody)) {
            return false;
        }

        MailTextBody that = (MailTextBody) o;

        if (mimeType != null ? !mimeType.equals(that.mimeType) : that.mimeType != null) {
            return false;
        }
        if (text != null ? !text.equals(that.text) : that.text != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        return result;
    }
}
