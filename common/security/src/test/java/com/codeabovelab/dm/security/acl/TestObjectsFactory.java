package com.codeabovelab.dm.security.acl;

import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.security.entity.*;

/**
 * utility factory for some entites
 */
public class TestObjectsFactory {
    
    private SidEntity owner;
    private ClassEntity classEntity;
    private TenantEntity tenantEntity;

    public void setOwner(SidEntity owner) {
        this.owner = owner;
    }

    public void setClassEntity(ClassEntity classEntity) {
        this.classEntity = classEntity;
    }
    
    void setTenantEntity(TenantEntity tenantEntity) {
        this.tenantEntity = tenantEntity;
    }
    
    public static TenantEntity createTenantEntity(long id, String name) {
        final TenantEntity e = new TenantEntity();
        e.setId(id);
        e.setName(name);
        return e;
    }
    
    ClassEntity createClassEntity(long id, String className) {
        ClassEntity e = new ClassEntity();
        e.setId(id);
        e.setClassName(className);
        return e;
    }
    
    ObjectIdentityEntity createObjectIdentityEntity(long id, long objectId) {
        final ObjectIdentityEntity e = new ObjectIdentityEntity();
        e.setId(id);
        e.setObjectId(objectId);
        e.setOwner(owner);
        e.setObjectClass(classEntity);
        return e;
    }

    SidEntity createSidPrincipalEntity(long id, String sid) {
        final SidEntity e = new SidEntity();
        e.setId(id);
        e.setSid(sid);
        e.setTenant(tenantEntity == null? MultiTenancySupport.NO_TENANT : tenantEntity.getName());
        e.setPrincipal(true);
        return e;
    }

    public static UserAuthDetails createUser(String userName) {
        UserAuthDetails details = new UserAuthDetails();
        details.setUsername(userName);
        details.setPassword("encoded");
        details.setEmail(userName + "test@test.te");
        details.setTitle("User " + userName);
        return details;
    }

}
