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

package com.codeabovelab.dm.cluman.ui;

import com.codeabovelab.dm.cluman.mail.MailNotificationsService;
import com.codeabovelab.dm.cluman.mail.MailSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collection;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping(value = "/ui/api/mail", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class EmailApi {

    private final MailNotificationsService mailNotificationsService;

    @RequestMapping(value = "/notices/", method = GET)
    public Collection<MailSubscription> list() {
        return mailNotificationsService.list();
    }

    @RequestMapping(value = "/notices/{eventSource}", method = GET)
    public MailSubscription get(@PathVariable("eventSource") String eventSource) {
        return mailNotificationsService.get(eventSource);
    }

    @RequestMapping(value = "/notices/", method = POST)
    public void add(@RequestBody @Valid MailSubscription subscription) {
        mailNotificationsService.put(subscription);
    }
}
