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

package com.codeabovelab.dm.common.security.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

/**
 * Data which is delivered in {@link AuthenticationData#getDetails()}
 */
public final class AuthenticationDetailsData {

    public static final class Builder {
        private String remoteAddress;
        private String sessionId;
        private String userDeviceId;
        private String returnAddress;

        public Builder() {
        }

        public String getRemoteAddress() {
            return remoteAddress;
        }

        public Builder remoteAddress(String remoteAddress) {
            setRemoteAddress(remoteAddress);
            return this;
        }

        public void setRemoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
        }

        public String getSessionId() {
            return sessionId;
        }

        public Builder sessionId(String sessionId) {
            setSessionId(sessionId);
            return this;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getUserDeviceId() {
            return userDeviceId;
        }

        public Builder userDeviceId(String userDeviceId) {
            setUserDeviceId(userDeviceId);
            return this;
        }

        /**
         * Set user device id
         * @param userDeviceId
         */
        public void setUserDeviceId(String userDeviceId) {
            this.userDeviceId = userDeviceId;
        }

        public String getReturnAddress() {
            return returnAddress;
        }

        /**
         * Address of current service instance for sending {@link com.codeabovelab.dm.common.userdevice.UserDeviceCommand }
         * @param returnAddress
         * @return
         */
        public Builder returnAddress(String returnAddress) {
            setReturnAddress(returnAddress);
            return this;
        }

        /**
         * Address of current service instance for sending {@link com.codeabovelab.dm.common.userdevice.UserDeviceCommand }
         * @param returnAddress
         */
        public void setReturnAddress(String returnAddress) {
            this.returnAddress = returnAddress;
        }

        /**
         * Address of current service instance for sending {@link com.codeabovelab.dm.common.userdevice.UserDeviceCommand }
         * @return
         */
        public AuthenticationDetailsData build() {
            return new AuthenticationDetailsData(this);
        }
    }

    private final String remoteAddress;
    private final String sessionId;
    private final String userDeviceId;
    private final String returnAddress;

    @JsonCreator
    public AuthenticationDetailsData(Builder builder) {
        this.remoteAddress = builder.remoteAddress;
        this.sessionId = builder.sessionId;
        this.userDeviceId = builder.userDeviceId;
        this.returnAddress = builder.returnAddress;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserDeviceId() {
        return userDeviceId;
    }

    /**
     * Address of current service instance for sending {@link com.codeabovelab.dm.common.userdevice.UserDeviceCommand }
     * @return
     */
    public String getReturnAddress() {
        return returnAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationDetailsData that = (AuthenticationDetailsData) o;
        return Objects.equals(remoteAddress, that.remoteAddress) &&
                Objects.equals(sessionId, that.sessionId) &&
                Objects.equals(returnAddress, that.returnAddress) &&
                Objects.equals(userDeviceId, that.userDeviceId) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteAddress, sessionId, userDeviceId);
    }

    @Override
    public String toString() {
        return "AuthenticationDetailsData{" +
          "remoteAddress='" + remoteAddress + '\'' +
          ", sessionId='" + sessionId + '\'' +
          ", returnAddress='" + returnAddress + '\'' +
          ", userDevice=" + userDeviceId +
          '}';
    }
}
