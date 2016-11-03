package com.codeabovelab.dm.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class JsonImmutableSecurityObjectTest {
    @Test
    public void testGrantedAuthority() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        final GrantedAuthorityImpl expected = new GrantedAuthorityImpl("test", "100l");
        String res = mapper.writeValueAsString(expected);
        GrantedAuthorityImpl actual = mapper.readValue(res, GrantedAuthorityImpl.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testUserDetails() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
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
}