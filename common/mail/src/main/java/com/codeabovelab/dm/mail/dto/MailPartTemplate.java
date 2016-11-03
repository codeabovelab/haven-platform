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

/**
 * Template of mail message part.
 */
public interface MailPartTemplate {
    MimeType getMime();

    /**
     * Used for persistence of template data. For text (and html) parts it return plain utf-8 text. For binary part it must
     * return efficiently encoded string (for example base64).
     * @return
     */
    String getData();
}
