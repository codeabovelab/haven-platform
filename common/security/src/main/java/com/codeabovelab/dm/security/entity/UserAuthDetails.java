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

import com.codeabovelab.dm.common.security.ExtendedUserDetails;
import com.codeabovelab.dm.common.security.OwnedByTenant;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * Models core user information retrieved by a {@link org.springframework.security.core.userdetails.UserDetailsService}. <p/>
 * <b>Note that this entity does not contains all user roles and other data, for retrieving correct UserDetails you
 * need to use {@link org.springframework.security.core.userdetails.UserDetailsService common service}.<b/>
 */
@Entity
@Cacheable
public class UserAuthDetails implements ExtendedUserDetails, OwnedByTenant {
    @Id
    @GeneratedValue
    private Long id;

    private String password;
    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    private String title;

    @ManyToMany(cascade= {},
            fetch = FetchType.EAGER, targetEntity = Authority.class)
    @JoinTable(
      name = "user_auth_details_authorities",
      joinColumns = @JoinColumn(name = "user_auth_details_id", referencedColumnName = "id")
    )
    private Set<Authority> authorities;
    /**
     * It attribute must be inaccessible because properties must be used only through
     * {@link com.codeabovelab.dm.security.user.UserPropertiesService}
     */
    @OneToMany(
            mappedBy = "userAuthDetails"
    )
    private Set<UserProperty> properties;

    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean enabled = true;

    @ManyToOne
    private TenantEntity tenant;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public Set<Authority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<Authority> authorities) {
        this.authorities = authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void eraseCredentials() {
        password = null;
    }

    /**
     * identity of tenant which own this user
     * @return
     */
    @Override
    public String getTenant() {
        return tenant == null? null : tenant.getName();
    }

    /**
     * identity of tenant which own this user
     * @param tenant
     */
    public void setTenantEntity(TenantEntity tenant) {
        this.tenant = tenant;
    }

    /**
     * identity of tenant which own this user
     * @return
     */
    public TenantEntity getTenantEntity() {
        return tenant;
    }

    /**
     * Do not use this  because user properties must be accessed via service
     * {@link com.codeabovelab.dm.security.user.UserPropertiesService}
     * @return
     */
    public Set<UserProperty> getProperties() {
        return properties;
    }

    /**
     * Do not use this  because user properties must be accessed via service
     * {@link com.codeabovelab.dm.security.user.UserPropertiesService}
     * @param properties
     */
    public void setProperties(Set<UserProperty> properties) {
        this.properties = properties;
    }

    private static SortedSet<GrantedAuthority> sortAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(authorities, "Cannot pass a null GrantedAuthority collection");
        // Ensure array iteration order is predictable (as per UserDetails.getAuthorities() contract and SEC-717)
        SortedSet<GrantedAuthority> sortedAuthorities =
                new TreeSet<>(new AuthorityComparator());

        for (GrantedAuthority grantedAuthority : authorities) {
            Assert.notNull(grantedAuthority, "GrantedAuthority list cannot contain any null elements");
            sortedAuthorities.add(grantedAuthority);
        }

        return sortedAuthorities;
    }

    private static class AuthorityComparator implements Comparator<GrantedAuthority>, Serializable {
        private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

        public int compare(GrantedAuthority g1, GrantedAuthority g2) {
            // Neither should ever be null as each entry is checked before adding it to the set.
            // If the authority is null, it is a custom authority and should precede others.
            if (g2.getAuthority() == null) {
                return -1;
            }

            if (g1.getAuthority() == null) {
                return 1;
            }

            return g1.getAuthority().compareTo(g2.getAuthority());
        }
    }

    /**
     * Returns {@code true} if the supplied object is a {@code User} instance with the
     * same {@code username} value.
     * <p>
     * In other words, the objects are equal if they have the same username, representing the
     * same principal.
     */
    @Override
    public boolean equals(Object rhs) {
        return rhs instanceof UserAuthDetails && username.equals(((UserAuthDetails) rhs).username);
    }

    /**
     * Returns the hashcode of the {@code username}.
     */
    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public String toString() {
        String s = "Username: " + username + "; " +
                "Enabled: " + enabled + "; " +
                "AccountNonExpired: " + accountNonExpired + "; " +
                "credentialsNonExpired: " + credentialsNonExpired + "; " +
                "AccountNonLocked: " + accountNonLocked + "; ";

        if (authorities != null && !authorities.isEmpty()) {
            s += "Granted Authorities: ";
            boolean first = true;
            for (GrantedAuthority auth : authorities) {
                if (!first) s += ",";
                first = false;
                s += auth;
            }
        } else  s += "Not granted any authorities";
        return s;
    }
}
