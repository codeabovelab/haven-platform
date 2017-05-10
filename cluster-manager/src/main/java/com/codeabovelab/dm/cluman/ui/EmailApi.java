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
import com.codeabovelab.dm.cluman.ui.model.UiEmailSubscription;
import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.ExtendedUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping(value = "/ui/api/mail/notices", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class EmailApi {

    private final MailNotificationsService mailNotificationsService;

    @RequestMapping(value = "/", method = GET)
    public List<UiEmailSubscription> list() {
        List<UiEmailSubscription> list = new ArrayList<>();
        mailNotificationsService.forEach(sub -> list.add(UiEmailSubscription.from(sub)));
        return list;
    }

    @RequestMapping(value = "/", method = POST)
    public void add(@RequestBody @Valid UiEmailSubscription subscription) {
        String emails = subscription.getEmail();
        checkAсcess(emails);
        MailSubscription ms = MailSubscription.builder()
          .severity(subscription.getSeverity())
          .email(emails)
          .eventSource(subscription.getEventSource())
          .build();
        mailNotificationsService.put(ms);
    }

    @RequestMapping(value = "/", method = DELETE)
    public void removeSubscribers(@RequestBody @Valid UiEmailSubscription subscription) {
        String email = subscription.getEmail();
        checkAсcess(email);
        mailNotificationsService.remove(subscription.getEventSource(), email);
    }

    private void checkAсcess(String emails) {
        ExtendedUserDetails eud = (ExtendedUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (Authorities.hasAnyOfAuthorities(eud, Authorities.ADMIN_ROLE)) {
            // admin can do anything
            return;
        }
        String userEmail = eud.getEmail();
        if (userEmail != null && userEmail.equalsIgnoreCase(emails)) {
            // user can modify own subscriptions
            return;
        }
        throw new SecurityException("User " + eud.getUsername() + " do not has access to add subscription");
    }
}
