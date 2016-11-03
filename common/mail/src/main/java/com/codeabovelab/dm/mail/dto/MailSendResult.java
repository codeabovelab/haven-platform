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
 * Status of mail sent.
 */
public class MailSendResult {

    public static class Builder {
        private MailHead head;
        private MailStatus status;
        private String error;

        public MailHead getHead() {
            return head;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = MailHeadImpl.class)
        public void setHead(MailHead head) {
            this.head = head;
        }

        public MailStatus getStatus() {
            return status;
        }

        public void setStatus(MailStatus status) {
            this.status = status;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public MailSendResult build() {
            return new MailSendResult(this);
        }
    }

    private final MailHead head;
    private final MailStatus status;
    private final String error;


    @JsonCreator
    public MailSendResult(Builder b) {
        this.head = b.head;
        Assert.notNull(this.head, "head is null");
        this.status = b.status;
        Assert.notNull(this.status, "status is null");
        this.error = b.error;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Head of original message.
     * @return
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = MailHeadImpl.class)
    public MailHead getHead() {
        return head;
    }

    /**
     * Result code. <p/>
     *
     * @return
     */
    public MailStatus getStatus() {
        return status;
    }

    /**
     * Error message. Can be null.
     * @return
     */
    public String getError() {
        return error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MailSendResult)) {
            return false;
        }

        MailSendResult that = (MailSendResult) o;

        if (error != null ? !error.equals(that.error) : that.error != null) {
            return false;
        }
        if (head != null ? !head.equals(that.head) : that.head != null) {
            return false;
        }
        if (status != that.status) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = head != null ? head.hashCode() : 0;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (error != null ? error.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MailSendResult{" +
          "head=" + head +
          ", status=" + status +
          ", error='" + error + '\'' +
          '}';
    }
}
