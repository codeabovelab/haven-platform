package com.codeabovelab.dm.security;

import com.codeabovelab.dm.security.acl.AclConfiguration;
import com.codeabovelab.dm.security.acl.TestObjectsFactory;
import com.codeabovelab.dm.security.entity.Authority;
import com.codeabovelab.dm.security.entity.AuthorityGroupEntity;
import com.codeabovelab.dm.security.entity.UserAuthDetails;
import com.codeabovelab.dm.security.repository.AuthorityGroupRepository;
import com.codeabovelab.dm.security.repository.AuthorityRepository;
import com.codeabovelab.dm.security.repository.UserRepository;
import com.codeabovelab.dm.security.sampleobject.SampleObjectsConfiguration;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * test user authorities groups api
 */
@SuppressWarnings("unchecked")
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringApplicationConfiguration(classes = {UserAuthoritiesGroupTest.TestConfiguration.class})
public class UserAuthoritiesGroupTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({AclConfiguration.class, SampleObjectsConfiguration.class})
    @ComponentScan(basePackageClasses = {UserAuthDetails.class, UserRepository.class})
    public static class TestConfiguration {
    }

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AuthorityGroupRepository authorityGroupRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private UserRepository userRepository;

    @Before
    public void init() {
        userRepository.save(TestObjectsFactory.createUser("user"));
    }

    @Test
    public void test() {

        Authority authority = new Authority();
        final String roleName = "NEW";
        final String userName = "user";
        authority.setRole(roleName);
        authority = authorityRepository.save(authority);

        final Collection<? extends GrantedAuthority> basicAuthorities = checkThatRoleUnexist(roleName, userName);

        AuthorityGroupEntity group = new AuthorityGroupEntity();
        group.setName("test_group");
        group = authorityGroupRepository.save(group);
        group.setAuthorities(new HashSet<>(Arrays.asList(authority)));
        group = authorityGroupRepository.save(group);
        group.setUsers(new HashSet<>(Arrays.asList(userRepository.findByUsername(userName))));
        group = authorityGroupRepository.save(group);

        checkThatRoleExist(roleName, userName, basicAuthorities);

        group.getUsers().remove(userRepository.findByUsername(userName));

        checkThatRoleUnexist(roleName, userName);
    }

    private Collection<? extends GrantedAuthority> checkThatRoleUnexist(String roleName, String userName) {
        Collection<? extends GrantedAuthority> basicAuthorities;UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
        assertNotNull(userDetails);
        basicAuthorities = userDetails.getAuthorities();
        assertThat(basicAuthorities, (Matcher) everyItem(hasProperty("authority", not(roleName))));
        return basicAuthorities;
    }

    private void checkThatRoleExist(String roleName, String userName, Collection<? extends GrantedAuthority> basicAuthorities) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
        assertNotNull(userDetails);
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertTrue(authorities.containsAll(basicAuthorities));
        assertTrue(authorities.size() - basicAuthorities.size() == 1);
        assertThat(authorities, (Matcher) hasItem(hasProperty("authority", is(roleName))));
    }
}
