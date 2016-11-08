package com.codeabovelab.dm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.codeabovelab.dm.common.json.JacksonUtils;
import com.codeabovelab.dm.common.security.dto.AceDataImpl;
import com.codeabovelab.dm.common.security.dto.AclDataImpl;
import com.codeabovelab.dm.common.security.TenantGrantedAuthoritySid;
import com.codeabovelab.dm.common.security.TenantPrincipalSid;
import org.junit.Test;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class DtoJsonTest {

    @Test
    public void test() throws Exception {
        ObjectMapper mapper = JacksonUtils.objectMapperBuilder();

        final AclDataImpl parent;
        {
            AclDataImpl.Builder b = AclDataImpl.builder();
            b.setId(101L);
            b.setOwner(new TenantPrincipalSid("principal2", "7l"));
            b.setEntriesInheriting(false);
            b.setObjectIdentity(new ObjectIdentityImpl("otherobjclass", "6789"));
            b.setEntries(Arrays.asList(
                    AceDataImpl.builder()
                            .id(1L)
                            .sid(new TenantGrantedAuthoritySid("ROLE_TESTER", "34l"))
                            .auditFailure(true)
                            .granting(false)
                            .permission(BasePermission.CREATE)
                            .build()/*, TODO implement serializing of cumulative permissions
                    AceDataImpl.builder()
                        .id(4l)
                        .auditSuccess(true)
                        .granting(true)
                        .permission(new CumulativePermission())
                        .build()*/
            ));
            parent = b.build();
        }

        AclDataImpl.Builder b = AclDataImpl.builder();
        b.setId(100L);
        b.setParentAclData(parent);
        b.setOwner(new TenantPrincipalSid("principal", "2l"));
        b.setEntriesInheriting(true);
        b.setObjectIdentity(new ObjectIdentityImpl("someobjectclass", "1234"));
        final AclDataImpl expected = b.build();

        String res = mapper.writeValueAsString(expected);
        final AclDataImpl actual = mapper.readValue(res, AclDataImpl.class);
        assertEquals(expected, actual);
    }
}