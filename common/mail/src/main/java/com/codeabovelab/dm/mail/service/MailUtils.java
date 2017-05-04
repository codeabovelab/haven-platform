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

package com.codeabovelab.dm.mail.service;

import com.codeabovelab.dm.mail.dto.MailBody;
import org.apache.commons.io.IOUtils;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import static org.springframework.http.MediaType.*;

public class MailUtils {

    private static final List<MimeType> mimeTypeSet =
            Arrays.asList(TEXT_HTML, TEXT_PLAIN, TEXT_XML, APPLICATION_XHTML_XML, APPLICATION_XML, APPLICATION_JSON);

    /**
     * Convert message body to text if possible, otherwise throw exception.
     * @param body
     * @return
     */
    public static String toPlainText(MailBody body) throws MailBadMessageException {
        MimeType mimeType = body.getMimeType();
        boolean containsMime = false;
        for (MimeType type : mimeTypeSet) {
            containsMime = containsMime || type.includes(mimeType);
        }
        if(!containsMime) {
            throw new MailBadMessageException("Message contains body with unsupported contentType: " + mimeType);
        }
        try(Reader r = body.getReader()) {
            return IOUtils.toString(r);
        } catch (IOException e) {
            throw new MailBadMessageException(e);
        }
    }
}
