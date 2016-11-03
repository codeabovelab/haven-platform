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

package com.codeabovelab.dm.cluman.mail;

import com.codeabovelab.dm.common.kv.mapping.KvMapping;
import com.codeabovelab.dm.cluman.model.Severity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableList;
import io.swagger.annotations.ApiParam;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Topic and severity is a key
 */
@Data
public class MailSubscription {

    @Data
    public static class Builder {
        @KvMapping
        private String eventSource;
        @KvMapping
        private Severity severity;
        @KvMapping
        private String title;
        //we store emails in set for prevent duplicates
        @KvMapping
        private final Set<String> emailRecipients = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        @KvMapping
        private String template;

        public Builder from(MailSubscription subs) {
            setEventSource(subs.getEventSource());
            setSeverity(subs.getSeverity());
            setTitle(subs.getTitle());
            setTemplate(subs.getTemplate());
            setEmailRecipients(subs.getEmailRecipients());
            return this;
        }

        public Builder emailRecipients(Collection<String> emailRecipients) {
            setEmailRecipients(emailRecipients);
            return this;
        }

        public void setEmailRecipients(Collection<String> emailRecipients) {
            this.emailRecipients.clear();
            if(emailRecipients != null) {
                this.emailRecipients.addAll(emailRecipients);
            }
        }

        public Builder addEmailRecipient(String email) {
            this.emailRecipients.add(email);
            return this;
        }

        public MailSubscription build() {
            return new MailSubscription(this);
        }
    }

    @NotNull
    private final String eventSource;
    @NotNull
    private final Severity severity;
    @ApiParam("Pass null for using default")
    private final String title;
    @NotNull
    //@Email.List TODO: validate emails
    private final List<String> emailRecipients;
    @ApiParam("Pass null for using default")
    private final String template;

    @JsonCreator
    public MailSubscription(Builder b) {
        this.eventSource = b.eventSource;
        this.severity = b.severity;
        this.title = b.title;
        this.emailRecipients = ImmutableList.copyOf(b.emailRecipients);
        this.template = b.template;
    }

    public static Builder builder() {
        return new Builder();
    }
}
