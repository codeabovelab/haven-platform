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

import com.codeabovelab.dm.common.utils.Functions;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 */
public class MailHeadImpl implements MailHead {

    public static class Builder implements MailHead {
        private String customId;
        private String from;
        private String subject;
        private String replyTo;
        private final List<String> to = new ArrayList<>();
        private final List<String> cc = new ArrayList<>();
        private final List<String> bcc = new ArrayList<>();
        private Date sentDate;

        @Override
        public String getCustomId() {
            return customId;
        }

        public Builder customId(String customId) {
            setCustomId(customId);
            return this;
        }

        public void setCustomId(String customId) {
            this.customId = customId;
        }

        @Override
        public String getFrom() {
            return from;
        }

        public Builder from(String from) {
            setFrom(from);
            return this;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        @Override
        public String getSubject() {
            return subject;
        }

        public Builder subject(String subject) {
            setSubject(subject);
            return this;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        @Override
        public String getReplyTo() {
            return replyTo;
        }

        public Builder replyTo(String replyTo) {
            setReplyTo(replyTo);
            return this;
        }

        public void setReplyTo(String replyTo) {
            this.replyTo = replyTo;
        }

        @Override
        public List<String> getTo() {
            return to;
        }

        public Builder to(List<String> to) {
            setTo(to);
            return this;
        }

        public void setTo(List<String> to) {
            this.to.clear();
            if(to != null) {
                this.to.addAll(to);
            }
        }

        @Override
        public List<String> getCc() {
            return cc;
        }


        public Builder cc(List<String> cc) {
            setCc(cc);
            return this;
        }

        public void setCc(List<String> cc) {
            this.cc.clear();
            if(cc != null) {
                this.cc.addAll(cc);
            }
        }

        @Override
        public List<String> getBcc() {
            return bcc;
        }

        public Builder bcc(List<String> bcc) {
            setBcc(bcc);
            return this;
        }

        public void setBcc(List<String> bcc) {
            this.bcc.clear();
            if(bcc != null) {
                this.bcc.addAll(bcc);
            }
        }

        @Override
        public Date getSentDate() {
            return sentDate;
        }

        public Builder sentDate(Date sentDate) {
            setSentDate(sentDate);
            return this;
        }

        public void setSentDate(Date sentDate) {
            this.sentDate = sentDate;
        }


        /**
         * Copy data from another object
         * @param message
         * @return
         */
        public Builder from(MailHead message) {
            return from(message, Functions.directFunc());
        }

        public Builder from(MailHead message, Function<Object, Object> iceptor) {
            setCustomId(message.getCustomId());
            setSentDate(Functions.intercept(iceptor, message.getSentDate()));
            setReplyTo(Functions.intercept(iceptor, message.getReplyTo()));
            setSubject(Functions.intercept(iceptor, message.getSubject()));
            setFrom(Functions.intercept(iceptor, message.getFrom()));
            setTo(Functions.intercept(iceptor, message.getTo()));
            setCc(Functions.intercept(iceptor, message.getCc()));
            setBcc(Functions.intercept(iceptor, message.getBcc()));
            return this;
        }

        public MailHeadImpl build() {
            return new MailHeadImpl(this);
        }
    }

    private final String customId;
    private final String from;
    private final String subject;
    private final String replyTo;
    private final List<String> to;
    private final List<String> cc;
    private final List<String> bcc;
    private final Date sentDate;

    @JsonCreator
    public MailHeadImpl(Builder builder) {
        this.customId = builder.customId;
        this.from = builder.from;
        this.subject = builder.subject;
        this.replyTo = builder.replyTo;
        this.to = Collections.unmodifiableList(new ArrayList<>(builder.to));
        this.cc = Collections.unmodifiableList(new ArrayList<>(builder.cc));
        this.bcc = Collections.unmodifiableList(new ArrayList<>(builder.bcc));
        this.sentDate = builder.sentDate;

        Assert.hasText(this.from, "'from' does not has text");
        Assert.hasText(this.subject, "'subject' does not has text");
        Assert.notEmpty(this.to, "'to' is empty");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getCustomId() {
        return customId;
    }

    @Override
    public String getFrom() {
        return from;
    }

    @Override
    public String getReplyTo() {
        return replyTo;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public List<String> getTo() {
        return to;
    }

    @Override
    public List<String> getCc() {
        return cc;
    }

    @Override
    public List<String> getBcc() {
        return bcc;
    }

    @Override
    public Date getSentDate() {
        return sentDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MailHeadImpl)) {
            return false;
        }

        MailHeadImpl that = (MailHeadImpl) o;

        if (customId != null ? !customId.equals(that.customId) : that.customId != null) {
            return false;
        }
        if (bcc != null ? !bcc.equals(that.bcc) : that.bcc != null) {
            return false;
        }
        if (cc != null ? !cc.equals(that.cc) : that.cc != null) {
            return false;
        }
        if (from != null ? !from.equals(that.from) : that.from != null) {
            return false;
        }
        if (sentDate != null ? !sentDate.equals(that.sentDate) : that.sentDate != null) {
            return false;
        }
        if (subject != null ? !subject.equals(that.subject) : that.subject != null) {
            return false;
        }
        if (to != null ? !to.equals(that.to) : that.to != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = from != null ? from.hashCode() : 0;
        result = 31 * result + (customId != null ? customId.hashCode() : 0);
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (cc != null ? cc.hashCode() : 0);
        result = 31 * result + (bcc != null ? bcc.hashCode() : 0);
        result = 31 * result + (sentDate != null ? sentDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MailHeadData{" +
          "customId='" + customId + '\'' +
          ", from='" + from + '\'' +
          ", subject='" + subject + '\'' +
          ", to=" + to +
          ", cc=" + cc +
          ", bcc=" + bcc +
          ", sentDate=" + sentDate +
          '}';
    }
}
