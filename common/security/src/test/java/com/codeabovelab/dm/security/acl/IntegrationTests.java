package com.codeabovelab.dm.security.acl;

import com.codeabovelab.dm.common.security.TenantPrincipalSid;
import com.codeabovelab.dm.security.TestsConfiguration;
import com.codeabovelab.dm.security.entity.UserAuthDetails;
import com.codeabovelab.dm.security.sampleobject.SampleObject;
import com.codeabovelab.dm.security.sampleobject.SampleObjectRepository;
import com.codeabovelab.dm.security.sampleobject.SampleObjectsConfiguration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.CumulativePermission;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@SpringApplicationConfiguration(classes = {SampleObjectsConfiguration.class, TestsConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IntegrationTests {
    @Autowired
    SampleObjectRepository repository;

    @Autowired
    MutableAclService aclService;

    @Autowired
    ObjectIdentityRetrievalStrategy idFunction;

    @Autowired
    UserDetailsService userDetailsService;

    private SampleObject soOne;
    private SampleObject soTwo;
    private SampleObject soThree;
    
    
    void setOwnerTo(final SampleObject so, String userLogin) {
        final ObjectIdentity oi = idFunction.getObjectIdentity(so);
        MutableAcl acl;
        try {
            acl = (MutableAcl) aclService.readAclById(oi);
        } catch (NotFoundException nfe) {
            acl = aclService.createAcl(oi);
        }
        TenantPrincipalSid tenantPrincipalSid = toPrincipalSid(userLogin);
        acl.setOwner(tenantPrincipalSid);
        aclService.updateAcl(acl);
    }

    private TenantPrincipalSid toPrincipalSid(String userLogin) {
        final UserAuthDetails user = (UserAuthDetails) userDetailsService.loadUserByUsername(userLogin);
        final TenantPrincipalSid tenantPrincipalSid = new TenantPrincipalSid(user.getUsername(), user.getTenant());
        return tenantPrincipalSid;
    }
    
    void delete(SampleObject sampleObject) {
        final long id = sampleObject.getId();
        repository.delete(id);
        assertTrue(!repository.exists(id));
    }

    /**
     * on <code>@Before</code> method a WithUserDetails annotation  - is not worked
     */
    @Before
    public void before() throws Exception {
        try(AutoCloseable c = Utilities.overrideAuth(userDetailsService, SampleUserDetailsService.USER_GLOBAL)) {
            soOne = repository.save(new SampleObject(10, "one"));
            setOwnerTo(soOne, SampleUserDetailsService.USER_ONE_TENANT_TWO);

            soTwo = repository.save(new SampleObject(20, "two"));
            setOwnerTo(soTwo, SampleUserDetailsService.USER_SECOND_TENANT_TWO);

            soThree = repository.save(new SampleObject(30, "three"));
            setOwnerTo(soThree, SampleUserDetailsService.USER_THIRD_TENANT_ONE);
        }
    }

    @Test
    @WithUserDetails(SampleUserDetailsService.USER_GLOBAL)
    public void testFindAllByGlobalAdmin() {
        assertEquals(set(soOne, soTwo, soThree), set(repository.findAll()));
    }

    @Test
    @WithUserDetails(SampleUserDetailsService.USER_ONE_TENANT_TWO)
    public void testFindAllByOneOwnedByFirstTenant() {
        assertEquals(set(soOne, soTwo), set(repository.findAll()));
    }
    
    @Test
    @WithUserDetails(SampleUserDetailsService.USER_SECOND_TENANT_TWO)
    public void testFindAllBySecondOwnedByFirstTenant() {
        assertEquals(set(soOne, soTwo), set(repository.findAll()));
    }
    
    @Test
    @WithUserDetails(SampleUserDetailsService.USER_THIRD_TENANT_ONE)
    public void testFindAllByThirdOwnedByTwoTenant() {
        // USER_THIRD_TENANT_ONE - see three objects because TENANT_ONE is parentTenant for TENANT_TWO
        assertEquals(set(soOne, soTwo, soThree), set(repository.findAll()));
    }
    
    @Test
    @WithUserDetails(SampleUserDetailsService.USER_SECOND_TENANT_TWO)
    public void testGrant() throws Exception {
        assertEquals(set(soOne, soTwo), set(repository.findAll()));

        try(AutoCloseable ac  = Utilities.overrideAuth(userDetailsService, SampleUserDetailsService.USER_THIRD_TENANT_ONE)) {
            //grant ACE for specified user
            final CumulativePermission perm = new CumulativePermission();
            perm.set(BasePermission.DELETE);
            perm.set(BasePermission.READ);
            grantTo(perm, soThree, SampleUserDetailsService.USER_SECOND_TENANT_TWO);
        }
        final List<SampleObject> list = (List<SampleObject>)repository.findAll();
        //USER_SECOND_TENANT_TWO can see three objects, because
        //one object owned by him
        assertTrue(list.contains(soTwo));
        //one object view by default behavior and tenant rule
        assertTrue(list.contains(soOne));
        //one object has ACE for this user
        assertTrue(list.contains(soThree));
        assertEquals(3, list.size());
    }
    
    @Test
    @WithUserDetails(SampleUserDetailsService.USER_SECOND_TENANT_TWO)
    public void testDelete() throws Exception {    
        delete(soTwo);
        try {
            //must be failed because owned by tenant_2
            delete(soThree);
            fail("we never must execute this line");
        } catch(AccessDeniedException e) {
            //all is ok
        }
    }

    private void grantTo(Permission permission, SampleObject obj, String userName) {
        final MutableAcl acl = (MutableAcl) aclService.readAclById(idFunction.getObjectIdentity(obj));
        acl.insertAce(acl.getEntries().size(), permission, toPrincipalSid(userName), true);
        aclService.updateAcl(acl);
    }

    private <T> Set<T> set(Iterable<T> items) {
        Set<T> set = new HashSet<>();
        for(T item : items) {
            set.add(item);
        }
        return set;
    }

    @SafeVarargs
    private static <T> Set<T> set(T ... items) {
        return new HashSet<>(Arrays.asList(items));
    }
}
