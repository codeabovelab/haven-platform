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

package com.codeabovelab.dm.common.security.token;

import java.util.Objects;

/**
 * The configuration for created token.
 */
public final class TokenConfiguration {
    private String deviceHash;
    private String userName;

    public TokenConfiguration() {
    }

    public TokenConfiguration(String deviceHash, String userName) {
        this.deviceHash = deviceHash;
        this.userName = userName;
    }

    public TokenConfiguration(String userName) {
        this.userName = userName;
    }

    /**
     * Hash of device, also may depend from user name, ip, mac address or imei. It used for binding token to
     * specific device, if you don't want this behavior, then leave this property null.
     * @return
     */
    public String getDeviceHash() {
        return deviceHash;
    }

    /**
     * Hash of device, also may depend from user name, ip, mac address or imei. It used for binding token to
     * specific device, if you don't want this behavior, then leave this property null.
     * @param deviceHash
     */
    public void setDeviceHash(String deviceHash) {
        this.deviceHash = deviceHash;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenConfiguration that = (TokenConfiguration) o;
        return Objects.equals(deviceHash, that.deviceHash) &&
                Objects.equals(userName, that.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceHash, userName);
    }
}
