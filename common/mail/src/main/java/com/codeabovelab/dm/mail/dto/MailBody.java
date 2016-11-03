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

import java.io.Reader;

/**
 * A common iface for any types of mail body. Expected only two types: text and text with attachments.
 */
public interface MailBody {
    MimeType getMimeType();

    /**
     * Stream with encoded data of this body.
     * @return
     */
    Reader  getReader();
}
