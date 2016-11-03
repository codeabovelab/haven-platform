package com.codeabovelab.dm.security.acl;

import com.codeabovelab.dm.common.security.TenantPrincipalSid;
import com.codeabovelab.dm.security.TestsConfiguration;
import com.codeabovelab.dm.security.entity.ObjectIdentityEntity;
import com.codeabovelab.dm.security.entity.TenantEntity;
import com.codeabovelab.dm.security.repository.TenantsRepository;
import com.codeabovelab.dm.security.sampleobject.SampleObjectsConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.transaction.Transactional;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {TestsConfiguration.class, SampleObjectsConfiguration.class})
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ObjectIdentityServiceTest {
    
    @Autowired
    private ObjectIdentityService service;
    
    @Autowired
    private ClassesService classesService;

    @Autowired
    private SidsService sidsService;
    
    @Autowired
    private TenantsRepository tenantsRepository;

    @Test
    @WithUserDetails(SampleUserDetailsService.USER_ONE_TENANT_TWO)
    public void test() {
        
        TestObjectsFactory factory = new TestObjectsFactory();
        factory.setClassEntity(classesService.getOrCreate(String.class.getName()));
        factory.setTenantEntity(TestObjectsFactory.createTenantEntity(1L, "root_tenant"));
        factory.setOwner(sidsService.getOrCreate(new TenantPrincipalSid(SecurityContextHolder.getContext().getAuthentication())));
        
        
        final ObjectIdentityEntity entityOne = service.save(factory.createObjectIdentityEntity(1L, 10L));
        
        final ObjectIdentityEntity entityTwo = service.save(factory.createObjectIdentityEntity(2L, 20L));
        
        assertEquals((Long) entityOne.getId(), service.getIdByIdentity(Utils.toIdentity(entityOne)));
        assertEquals((Long) entityTwo.getId(), service.getIdByIdentity(Utils.toIdentity(entityTwo)));
        
        assertEquals(entityOne, service.getByIdentity(Utils.toIdentity(entityOne)));
        assertEquals(entityTwo, service.getByIdentity(Utils.toIdentity(entityTwo)));
    }
    
}
