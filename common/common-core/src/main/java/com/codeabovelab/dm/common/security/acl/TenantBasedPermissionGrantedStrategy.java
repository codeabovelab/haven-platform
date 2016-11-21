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

import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.common.security.OwnedByTenant;
import com.codeabovelab.dm.common.security.dto.PermissionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

/**
 * the strategy which implement permission granted mechanism with considering 
 * of tenant user attribute <p/>
 * TODO need refactoring (isGranted & getPermission)
 */
public final class TenantBasedPermissionGrantedStrategy implements ExtPermissionGrantingStrategy {

    private final PermissionGrantingJudge defaultBehavior;
    
    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    TenantsService<?> tenantsService;

    @Autowired
    AuditLogger auditLogger;

    public TenantBasedPermissionGrantedStrategy(PermissionGrantingJudge defaultBehavior) {
        this.defaultBehavior = defaultBehavior;
    }

    @Override
    public boolean isGranted(Acl acl, List<Permission> permission, List<Sid> sids, boolean administrativeMode) {
        Assert.notNull(tenantsService, "tenantsService is null");
        Assert.notNull(userDetailsService, "userDetailsService is null");

        final Sid ownerSid = acl.getOwner();
        final String ownerTenantId = getTenantFromSid(ownerSid);
        if(ownerTenantId == MultiTenancySupport.NO_TENANT) {
            throw new RuntimeException("Can not retrieve tenant from acl owner: acl.objectIdentity=" + acl.getObjectIdentity().getIdentifier());
        }

        final String currentPrincipalTenant = getPrincipalSidTenant(sids);
        
        PermissionGrantingContext pgc = new PermissionGrantingContext(this, ownerSid, currentPrincipalTenant);
        // below code based on DefaultPermissionGrantingStrategy
        final List<AccessControlEntry> aces = acl.getEntries();

        
        //when allow we need set this flag to true and optionally set aceWhichBreak
        boolean allow = false;
        AccessControlEntry aceWhichBreak = null;

        //we use for with indexes for in order to avoid creating iterators
        //  which will be adding some memory overhead in 3 nested loops
        mainLoop:
        for(int permIndex = 0; permIndex < permission.size(); ++permIndex) {
            final Permission p = permission.get(permIndex);
            for(int sidIndex = 0; sidIndex < sids.size(); ++sidIndex) {
                final Sid sid = sids.get(sidIndex);
                
                pgc.setHasAces(!aces.isEmpty());
                pgc.setCurrentSid(sid);

                //default behaviour
                PermissionData defaultPerm = defaultBehavior.getPermission(pgc);
                if((p.getMask() & defaultPerm.getMask()) == p.getMask()) {
                    allow = true;
                    break mainLoop;
                }
                
                for(int aceIndex = 0; aceIndex < aces.size(); ++ aceIndex) {
                    AccessControlEntry ace = aces.get(aceIndex);
                    Sid aceSid = ace.getSid();
                    final String aceTenant = getTenantFromSid(aceSid);
                    // we allow null tenants because it way to grant access to any tenant (it rare need)
                    //root SIDs consume all ACE
                    if(aceTenant != null && !pgc.getCurrentTenants().contains(aceTenant)) {
                        continue;
                    }
                    if(!compareSids(sid, aceSid)) {
                        continue;
                    }
                    boolean maskCompared = comparePermissions(p, ace);
                    if(!maskCompared) {
                        continue;
                    }
                    // Found a matching ACE, so its authorization decision will prevail
                    if(aceWhichBreak == null) {
                        // Store for auditing reasons
                        aceWhichBreak = ace;
                    }

                    allow = ace.isGranting();
                    break mainLoop; // exit aces loop
                }
            }
        }
        if(allow) {
            // Success
            
            // audit logger is not allow null ACE, but if access allowed by 
            //   default behaviour then we don't have an ACE
            if(!administrativeMode && aceWhichBreak != null) {
                auditLogger.logIfNeeded(true, aceWhichBreak);
            }

            return true;
        } else if(aceWhichBreak != null) {
            // We found an ACE to reject the request at this point, as no
            // other ACEs were found that granted a different permission
            if(!administrativeMode) {
                auditLogger.logIfNeeded(false, aceWhichBreak);
            }

            return false;
        }

        // No matches have been found so far
        if(acl.isEntriesInheriting() && (acl.getParentAcl() != null)) {
            // We have a parent, so let them try to find a matching ACE
            return acl.getParentAcl().isGranted(permission, sids, false);
        } else {
            // We either have no parent, or we're the uppermost parent
            throw new NotFoundException("Unable to locate a matching ACE for passed permissions: " + permission + " and SIDs: " + sids);
        }
    }

    private boolean comparePermissions(final Permission p, final AccessControlEntry ace) {
        // in original behavior see https://jira.spring.io/browse/SEC-1140 masks compared by '==', 
        // but we can use mask bitwise comparsion for allow-ACEs and its inversion for deny-ACEs
        
        final boolean maskCompared;
        final int requestedMask = p.getMask();
        final int aceMask = ace.getPermission().getMask();
        if(ace.isGranting()) {
            //if all requisted bits coincided then allow
            maskCompared = (aceMask & requestedMask) == requestedMask;
        } else {
            //if at least one bit is coincided then deny
            maskCompared = (aceMask & requestedMask) != 0;
        }
        return maskCompared;
    }

    private String getPrincipalSidTenant(List<Sid> sids) throws IllegalStateException {
        //first we need detect current principal tenant for using it with
        //  GrantedAuthority which does not contains tenantId
        PrincipalSid principalSid = null;
        for(int sidIndex = 0; sidIndex < sids.size(); ++sidIndex) {
            final Sid sid = sids.get(sidIndex);
            if(sid instanceof PrincipalSid) {
                if(principalSid != null && !principalSid.equals(sid)) {
                    throw new IllegalStateException("We found more than one PrincipalSid: " + principalSid + " and " + sid);
                }
                principalSid = (PrincipalSid)sid;
            }
        }
        if(principalSid == null) {
            throw new IllegalStateException("We can not find PrincipalSid");
        }
        String tenant = getTenantFromSid(principalSid);
        if(tenant == MultiTenancySupport.NO_TENANT) {
            throw new IllegalStateException("No 'tenant' found in PrincipalSid");
        }
        return tenant;
    }

    /**
     * primitive naive realization of `tenantId=F(principal)` - function
     * <p/>
     * in future we can use `PrincipalSid` implementation which contains also
     * tenantId value
     *
     * @param sid
     * @return
     */
    String getTenantFromSid(final Sid sid) {
        if(sid == null) {
            return MultiTenancySupport.NO_TENANT;
        }
        if(sid instanceof OwnedByTenant) {
            return ((OwnedByTenant)sid).getTenant();
        }
        if(!(sid instanceof PrincipalSid)) {
            return MultiTenancySupport.NO_TENANT;
        }
        final PrincipalSid owner = (PrincipalSid)sid;
        final OwnedByTenant user = (OwnedByTenant)userDetailsService.loadUserByUsername(owner.getPrincipal());
        final String tenantId = user.getTenant();
        return tenantId;
    }

    @Override
    public PermissionData getPermission(Acl acl, List<Sid> sids) {
        Assert.notNull(tenantsService, "tenantsService is null");
        Assert.notNull(userDetailsService, "userDetailsService is null");

        final Sid ownerSid = acl.getOwner();
        final String ownerTenantId = getTenantFromSid(ownerSid);
        if(ownerTenantId == MultiTenancySupport.NO_TENANT) {
            throw new RuntimeException("Can not retrieve tenant from acl owner: acl.objectIdentity=" + acl.getObjectIdentity().getIdentifier());
        }

        final String currentPrincipalTenant = getPrincipalSidTenant(sids);

        PermissionGrantingContext pgc = new PermissionGrantingContext(this, ownerSid, currentPrincipalTenant);
        // below code based on DefaultPermissionGrantingStrategy
        final List<AccessControlEntry> aces = acl.getEntries();


        PermissionData.Builder pb = PermissionData.builder();
        // !! not use foreach here
        for(int sidIndex = 0; sidIndex < sids.size(); ++sidIndex) {
            final Sid sid = sids.get(sidIndex);

            pgc.setHasAces(!aces.isEmpty());
            pgc.setCurrentSid(sid);

            pb.add(defaultBehavior.getPermission(pgc));

            for(int aceIndex = 0; aceIndex < aces.size(); ++ aceIndex) {
                AccessControlEntry ace = aces.get(aceIndex);
                Sid aceSid = ace.getSid();
                final String aceTenant = getTenantFromSid(aceSid);
                //root SIDs consume all ACE
                if(aceTenant != null && !pgc.getCurrentTenants().contains(aceTenant)) {
                    continue;
                }
                if(!compareSids(sid, aceSid)) {
                    continue;
                }

                Permission acep = ace.getPermission();
                if(ace.isGranting()) {
                    pb.add(acep);
                } else {
                    pb.remove(acep);
                }
            }
        }
        //TODO handle ACL inheriting
        return pb.build();
    }



    /**
     * Note that position of SIDs is important
     * @param authSid
     * @param aclSid
     * @return
     */
    private boolean compareSids(Sid authSid, Sid aclSid) {
        if(MultiTenancySupport.isNoTenant(aclSid)) {
            // acl sid can has no tenant, we must consider this
            // not that null tenant mean that it common rule for any GrantedAuthorities of tenants
            if(authSid instanceof GrantedAuthoritySid) {
                return (aclSid instanceof GrantedAuthoritySid) && Objects.equals(
                  ((GrantedAuthoritySid) authSid).getGrantedAuthority(),
                  ((GrantedAuthoritySid) aclSid).getGrantedAuthority()
                );
            }
            if(authSid instanceof PrincipalSid) {
                return (aclSid instanceof PrincipalSid) && Objects.equals(
                  ((PrincipalSid) authSid).getPrincipal(),
                  ((PrincipalSid) aclSid).getPrincipal()
                );
            }
        }
        // there a unsupported sids or its has tenant, compare its as usual objects
        return aclSid.equals(authSid);
    }

}
