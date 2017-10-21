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
import com.codeabovelab.dm.cluman.ui.model.UiUserSubscription;
import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.ExtendedUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public List<UiUserSubscription> list() {
        List<UiUserSubscription> list = new ArrayList<>();
        mailNotificationsService.forEach(sub -> list.add(UiUserSubscription.from(sub)));
        return list;
    }

    @RequestMapping(value = "/", method = POST)
    public void add(@RequestBody @Valid UiUserSubscription subscription) {
        String user = subscription.getUser();
        if(user == null) {
            user = getCurrentUser().getUsername();
        } else {
            checkAccess(user);
        }
        MailSubscription ms = MailSubscription.builder()
          .severity(subscription.getSeverity())
          .user(user)
          .eventSource(subscription.getEventSource())
          .build();
        mailNotificationsService.put(ms);
    }

    @RequestMapping(value = "/", method = DELETE)
    public void remove(@RequestBody @Valid UiUserSubscription subscription) {
        String user = subscription.getUser();
        if(user == null) {
            user = getCurrentUser().getUsername();
        } else {
            checkAccess(user);
        }
        mailNotificationsService.remove(subscription.getEventSource(), user);
    }

    private void checkAccess(String modifiedUser) {
        ExtendedUserDetails eud = getCurrentUser();
        if (Authorities.hasAnyOfAuthorities(eud, Authorities.ADMIN_ROLE)) {
            // admin can do anything
            return;
        }
        String user = eud.getUsername();
        if (user != null && user.equals(modifiedUser)) {
            // user can modify own subscriptions
            return;
        }
        throw new SecurityException("User " + user + " do not has access to add subscription");
    }

    private ExtendedUserDetails getCurrentUser() {
        return (ExtendedUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
