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

import java.util.Date;
import java.util.List;

/**
 * Heading of mail message.
 */
public interface MailHead {

    /**
     * User defined id for mail, it used only by service client for identification mail in status events.
     * @return
     */
    String getCustomId();

    String getFrom();

    String getReplyTo();

    List<String> getTo();

    List<String> getCc();

    List<String> getBcc();

    Date getSentDate();

    String getSubject();

}
