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

package com.codeabovelab.dm.security.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(
  name = "token",
  uniqueConstraints = @UniqueConstraint(columnNames = "token")
)
public class Token {
	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private String userName;

	@Column
	private String deviceHash;

	@Column(nullable = false)
	private String token;

	@Column(nullable = false)
	private Date creationDate;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

    /**
     * Hash of device, also may depend from user name, ip, mac address or imei. It used for binding token to
     * specific device, if you don't want this behavior, then leave this property null.
     * @see com.codeabovelab.dm.common.security.token.TokenConfiguration#getDeviceHash()
     * @return
     */
    public String getDeviceHash() {
        return deviceHash;
    }

    /**
     * Hash of device, also may depend from user name, ip, mac address or imei. It used for binding token to
     * specific device, if you don't want this behavior, then leave this property null.
     * @see com.codeabovelab.dm.common.security.token.TokenConfiguration#getDeviceHash()
     * @param deviceHash
     */
    public void setDeviceHash(String deviceHash) {
        this.deviceHash = deviceHash;
    }

    public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

    @Override
    public String toString() {
        return "AccessToken{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", deviceHash='" + deviceHash + '\'' +
                ", token='" + token + '\'' +
                ", creationDate=" + creationDate +
                '}';
    }
}
