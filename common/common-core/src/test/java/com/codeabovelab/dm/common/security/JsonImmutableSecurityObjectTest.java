package com.codeabovelab.dm.common.security;

import com.codeabovelab.dm.common.security.acl.AceSource;
import com.codeabovelab.dm.common.security.acl.AclSource;
import com.codeabovelab.dm.common.security.acl.TenantSid;
import com.codeabovelab.dm.common.security.dto.ObjectIdentityData;
import com.codeabovelab.dm.common.security.dto.PermissionData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.security.acls.model.Sid;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class JsonImmutableSecurityObjectTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testGrantedAuthority() throws Exception {
        final GrantedAuthorityImpl expected = new GrantedAuthorityImpl("test", "100l");
        String res = mapper.writeValueAsString(expected);
        GrantedAuthorityImpl actual = mapper.readValue(res, GrantedAuthorityImpl.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testUserDetails() throws Exception {
        final ExtendedUserDetailsImpl.Builder b = new ExtendedUserDetailsImpl.Builder();
        b.setUsername("one");
        b.setPassword("secret");
        b.setTitle("test user");
        b.setEmail("e@e.e");
        b.setEnabled(true);
        b.setAccountNonLocked(true);
        b.setAccountNonExpired(true);
        b.setCredentialsNonExpired(true);
        b.setTenant("34l");
        b.setAuthorities(Arrays.asList(new GrantedAuthorityImpl("ga1", "3l"), new GrantedAuthorityImpl("ga2", "6l")));
        ExtendedUserDetailsImpl expected = b.build();
        String res = mapper.writeValueAsString(expected);
        ExtendedUserDetailsImpl actual = mapper.readValue(res, ExtendedUserDetailsImpl.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testAclSource() throws Exception {
        ObjectIdentityData oid = new ObjectIdentityData("type", "id");
        TenantSid owner = new TenantPrincipalSid("user", "sometenant");
        TenantSid ownerGroup = new TenantGrantedAuthoritySid("ROLE_USER", "sometenant");
        AclSource.Builder b = AclSource.builder()
          .owner(owner)
          .objectIdentity(oid);
        testJson(b.build());

        b.owner(ownerGroup);
        testJson(b.build());

        b.addEntry(AceSource.builder()
          .id("1")
          .sid(owner)
          .permission(PermissionData.builder().add(Action.READ).add(Action.UPDATE).build())
          .granting(true)
          .build());
        b.addEntry(AceSource.builder()
          .id("2")
          .sid(owner)
          .permission(Action.ALTER_INSIDE)
          .granting(true)
          .build());
        testJson(b.build());
    }

    private void testJson(Object o) throws Exception {
        String s = mapper.writeValueAsString(o);
        System.out.println("Write object: " + o + "\n as json:" + s);
        Object readed = mapper.readValue(s, o.getClass());
        assertEquals(o, readed);
    }
}