package com.codeabovelab.dm.security.acl;

import com.codeabovelab.dm.common.security.TenantGrantedAuthoritySid;
import com.codeabovelab.dm.security.TestsConfiguration;
import com.codeabovelab.dm.security.entity.TenantEntity;
import com.codeabovelab.dm.security.repository.TenantsRepository;
import com.codeabovelab.dm.security.sampleobject.SampleObjectsConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.*;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@SpringApplicationConfiguration(classes = {TestsConfiguration.class, SampleObjectsConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MutableJpaAclServiceTest {
    
    @Autowired
    private MutableJpaAclService aclService;
    
    @Autowired
    private TenantsRepository tenantsRepository;
        
    @Test
    @WithUserDetails(SampleUserDetailsService.USER_ONE_TENANT_TWO)
    public void testSomeMethod() {
        final TenantEntity tenant = tenantsRepository.save(new TenantEntity().id(1L).name("one"));
        
        final ObjectIdentity oi1 = new ObjectIdentityImpl(String.class, 1L);
        final ObjectIdentity oi2 = new ObjectIdentityImpl(String.class, 2L);
        final ObjectIdentity oi3 = new ObjectIdentityImpl(String.class, 3L);
        
        //create ACLs for objects
        aclService.createAcl(oi1);
        aclService.createAcl(oi2);
        
        //test that ACLs is created
        assertNotNull(aclService.readAclById(oi1));
        assertNotNull(aclService.readAclById(oi2));
        try {
            //test that ACLs for third object is NotFound
            assertNull(aclService.readAclById(oi3));
            fail("NotFoundException must be thrown");
        } catch(NotFoundException e) {
            //as expected
        }
        
        //test that ACL for first object has not childs
        assertTrue(aclService.findChildren(oi1).isEmpty());
        
        //add child for first object ACL
        {
            final Acl parent = aclService.readAclById(oi1);
            final MutableAcl child = (MutableAcl) aclService.readAclById(oi2);
            child.setParent(parent);
            aclService.updateAcl(child);
        }
        
        //test that ACL for first object now have one child 
        {
            final List<ObjectIdentity> childs = aclService.findChildren(oi1);
            assertEquals(childs.size(), 1);
            assertEquals(childs.get(0), oi2);
        }
        
        //add ACE for ROLE_TEST on first object ACL
        final TenantGrantedAuthoritySid testRole = new TenantGrantedAuthoritySid("ROLE_TEST", tenant.getName());
        {
            final MutableAcl oi1Acl = (MutableAcl) aclService.readAclById(oi1);
            oi1Acl.insertAce(0, BasePermission.READ, testRole, true);
            aclService.updateAcl(oi1Acl);
        }
        
        //test taht ACE for ROLE_TEST on first object ACL is exist
        {
            final Acl acl = aclService.readAclById(oi1);
            final List<AccessControlEntry> entries = acl.getEntries();
            assertEquals(1, entries.size());
            final AccessControlEntry ace = entries.get(0);
            assertEquals(BasePermission.READ, ace.getPermission());
            assertTrue(ace.isGranting());
        }
    }
}
