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

package com.codeabovelab.dm.common.security.acl;

import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 */
public class AclUtils {

    public static boolean isSidLoaded(List<Sid> loadedSids, List<Sid> sids) {
        // If loadedSides is null, this indicates all SIDs were loaded
        // Also return true if the caller didn't specify a SID to find
        if ((loadedSids == null) || (sids == null) || (sids.size() == 0)) {
            return true;
        }

        // This ACL applies to a SID subset only. Iterate to check it applies.
        for (Sid sid: sids) {
            boolean found = false;

            for (Sid loadedSid : loadedSids) {
                if (sid.equals(loadedSid)) {
                    // this SID is OK
                    found = true;

                    break; // out of loadedSids for loop
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    public static void buildEntries(Acl acl, Collection<?> from, Consumer<AccessControlEntry> to) {
        for(Object object: from) {
            AccessControlEntry ace;
            if(object instanceof AceSource) {
                ace = new AccessControlEntryImpl.Builder().from((AccessControlEntry) object).acl(acl).build();
            } else if(object instanceof AccessControlEntryImpl.Builder) {
                ace = ((AccessControlEntryImpl.Builder)object).acl(acl).build();
            } else if(object instanceof AccessControlEntry) {
                ace = (AccessControlEntry) object;
            } else {
                throw new IllegalArgumentException(object + " must be an instance of " + AccessControlEntry.class + " or it's builder");
            }
            to.accept(ace);
        }
    }

    public static String toId(ObjectIdentity object) {
        Assert.notNull(object, "ObjectIdentity is null");
        Object idsrc = object.getIdentifier();
        Assert.notNull(idsrc, "identifier is null");
        String type = object.getType();
        return toId(type, idsrc);
    }

    public static String toId(String type, Object id) {
        if(id == null) {
            return type + ":";
        }
        return type + ":" + id.toString();
    }

    /**
     * Make id fot type of identity (ignore object.identifier).
     * @param object
     * @return
     */
    public static String toTypeId(ObjectIdentity object) {
        Assert.notNull(object, "ObjectIdentity is null");
        String type = object.getType();
        return toTypeId(type);
    }

    public static String toTypeId(String type) {
        return toId(type, null);
    }

    public static ObjectIdentity fromId(String id) {
        String[] split = StringUtils.split(id, ":");
        return new ObjectIdentityImpl(split[0], split[1]);
    }
}
