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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.util.Assert;

/**
 * Data of mail
 */
public class MailMessageImpl implements MailMessage {

    public static class Builder implements MailMessage {
        private MailHead head;
        private MailBody body;

        @Override
        public MailHead getHead() {
            return head;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = MailHeadImpl.class)
        public void setHead(MailHead head) {
            this.head = head;
        }

        @Override
        public MailBody getBody() {
            return body;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = MailTextBody.class)
        public void setBody(MailBody body) {
            this.body = body;
        }

        public Builder from(MailMessage message) {
            setHead(message.getHead());
            setBody(message.getBody());
            return this;
        }

        public MailMessageImpl build() {
            return new MailMessageImpl(this);
        }
    }

    private final MailHead head;
    private final MailBody body;

    @JsonCreator
    public MailMessageImpl(Builder builder) {
        this.head = builder.head;
        this.body = builder.body;

        Assert.notNull(this.head, "'head' is null");
        Assert.notNull(this.body, "'body' is null");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = MailHeadImpl.class)
    public MailHead getHead() {
        return head;
    }

    @Override
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = MailTextBody.class)
    public MailBody getBody() {
        return body;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MailMessageImpl)) {
            return false;
        }

        MailMessageImpl that = (MailMessageImpl) o;

        if (head != null ? !head.equals(that.head) : that.head != null) {
            return false;
        }
        if (body != null ? !body.equals(that.body) : that.body != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = head != null ? head.hashCode() : 0;
        result = 31 * result + (body != null ? body.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MailMessageData{" +
          "head='" + head + '\'' +
          ", body=" + body +
          '}';
    }
}
