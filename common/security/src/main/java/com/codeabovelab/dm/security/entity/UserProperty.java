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

/**
 */
@Entity
@Table(name = "user_props")
public class UserProperty {
    @Id
    @GeneratedValue
    private Long id;

    /**
     * we need CascadeType.PERSIST op for reset UserAuthDetails cache
     */
    @ManyToOne(cascade = {CascadeType.PERSIST}, optional = false)
    @JoinColumn(nullable = false, name = "user_auth_details_id")
    private UserAuthDetails userAuthDetails;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String data;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserAuthDetails getUserAuthDetails() {
        return userAuthDetails;
    }

    public void setUserAuthDetails(UserAuthDetails userAuthDetails) {
        this.userAuthDetails = userAuthDetails;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }



    @Override
    public String toString() {
        return "UserProperty{" +
          "name='" + name + '\'' +
          ", data='" + data + '\'' +
          '}';
    }
}
