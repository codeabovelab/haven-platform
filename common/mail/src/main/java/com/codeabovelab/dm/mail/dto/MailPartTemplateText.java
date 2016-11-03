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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.MimeType;

/**
 * Simple part of mail. It contains text data with specified mime.
 */
public class MailPartTemplateText implements MailPartTemplate {
    private final MimeType mime;
    private final String data;

    @JsonCreator
    public MailPartTemplateText(@JsonProperty("mime") MimeType mime,
                                @JsonProperty("data") String data) {
        this.mime = mime;
        this.data = data;
    }

    @Override
    public MimeType getMime() {
        return mime;
    }

    @Override
    public String getData() {
        return data;
    }
}
