package com.codeabovelab.dm.cluman.security;

import com.codeabovelab.dm.cluman.DockerServiceMock;
import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.DeleteContainerArg;
import com.codeabovelab.dm.cluman.cluster.docker.model.ContainerDetails;
import com.codeabovelab.dm.cluman.cluster.docker.model.CreateContainerCmd;
import com.codeabovelab.dm.cluman.configuration.SecurityConfiguration;
import com.codeabovelab.dm.cluman.model.DockerServiceInfo;
import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.GrantedAuthorityImpl;
import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.common.security.acl.ExtPermissionGrantingStrategy;
import com.codeabovelab.dm.common.security.dto.AuthenticationData;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 */
@Slf4j
@ActiveProfiles("test_docker_security")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DockerServiceSecurityWrapperTest.AppConfiguration.class)
public class DockerServiceSecurityWrapperTest {

    public static final String TESTCLUSTER = "testcluster";
    public static final String USER = "user";
    public static final String ROOT = MultiTenancySupport.ROOT_TENANT;

    @Import({SecurityConfiguration.class})
    @Configuration
    @EnableAutoConfiguration(exclude = EndpointWebMvcAutoConfiguration.class)
    public static class AppConfiguration {
        @Primary
        @Bean
        AccessContextFactory aclContextFactory(ConfigurableAclService aclService, ExtPermissionGrantingStrategy pgs, SidRetrievalStrategy sidRetrievalStrategy) {
            return new AccessContextFactory(aclService, pgs, sidRetrievalStrategy);
        }
    }

    @Autowired
    private AccessContextFactory acf;

    private DockerService makeDockerService(String name) {
        return new DockerServiceSecurityWrapper(acf, new DockerServiceMock(DockerServiceInfo.builder()
          .name(name).build()));
    }

    private DockerService service ;
    private DockerService ownedService;

    @Autowired
    private UserDetailsService userDetailsService;

    private UserDetails user;
    private UserDetails otherUser;

    @Before
    public void before() {
        service = makeDockerService(TESTCLUSTER);
        ownedService = makeDockerService("ownedCluster");
        user = userDetailsService.loadUserByUsername("user");
        otherUser = userDetailsService.loadUserByUsername("otherUser");
    }

    @Test
    public void testRoles() {
        try {
            create();
            fail("create must be failed");
        } catch (AccessDeniedException e) {
            log.debug("Expect fail: {}", e.getMessage());
            assertEquals("No credentials in context.", e.getMessage());
        }

        try(TempAuth auth = TempAuth.open(createUserAuth())) {
            create();
            fail("create must be failed");
        } catch (AccessDeniedException e) {
            log.debug("Expect fail: {}", e.getMessage());
            assertTrue(e.getMessage().contains("denied"));
        }

        try(TempAuth auth = TempAuth.open(createUserAuth(Authorities.USER_ROLE, SecuredType.CONTAINER.admin()))) {
            create();
        }


        try(TempAuth auth = TempAuth.asSystem()) {
            create();
        }
    }

    @Test
    public void testAcl() {

        try(TempAuth auth = TempAuth.open(createAuthFromDetails(user))) {
            CreateContainerCmd ccc = new CreateContainerCmd();
            ccc.setImage("testimage");
            ccc.setName("cont1");
            service.createContainer(ccc);
        }

        try(TempAuth auth = TempAuth.open(createAuthFromDetails(user))) {
            ContainerDetails cont1 = service.getContainer("cont1");
            log.debug("Container: {}", cont1);
        }

        try(TempAuth auth = TempAuth.open(createAuthFromDetails(otherUser))) {
            ContainerDetails cont1 = service.getContainer("cont1");
            fail("container must not be accessible");
        } catch (AccessDeniedException e) {
            log.debug("Expect fail: {}", e.getMessage());
            assertTrue(e.getMessage().contains("denied"));
        }

        try(TempAuth auth = TempAuth.open(createAuthFromDetails(otherUser))) {
            service.getClusterConfig();
            fail("cluster must not be accessible");
        } catch (AccessDeniedException e) {
            log.debug("Expect fail: {}", e.getMessage());
            assertTrue(e.getMessage().contains("denied"));
        }

        try(TempAuth auth = TempAuth.open(createAuthFromDetails(user))) {
            service.deleteContainer(DeleteContainerArg.builder().id("cont1").build());
            fail("container must not be accessible");
        } catch (AccessDeniedException e) {
            log.debug("Expect fail: {}", e.getMessage());
            assertTrue(e.getMessage().contains("denied"));
        }

    }

    @Test
    public void testAclCluster() {

        final Authentication auth = createAuthFromDetails(otherUser);
        try(TempAuth ta = TempAuth.open(auth)) {
            CreateContainerCmd ccc = new CreateContainerCmd();
            ccc.setImage("testimage");
            ccc.setName("cont1");
            ownedService.createContainer(ccc);

            ContainerDetails cont1 = ownedService.getContainer("cont1");
            log.debug("Container: {}", cont1);

            ownedService.deleteContainer(DeleteContainerArg.builder().id("cont1").build());
        }

        try(TempAuth ta = TempAuth.open(createAuthFromDetails(user))) {
            ContainerDetails cont1 = ownedService.getContainer("cont1");
            fail("container must not be accessible");
        } catch (AccessDeniedException e) {
            log.debug("Expect fail: {}", e.getMessage());
            assertTrue(e.getMessage().contains("denied"));
        }

        try(TempAuth ta = TempAuth.open(createAuthFromDetails(user))) {
            ownedService.getClusterConfig();
            fail("cluster must not be accessible");
        } catch (AccessDeniedException e) {
            log.debug("Expect fail: {}", e.getMessage());
            assertTrue(e.getMessage().contains("denied"));
        }

    }

    private Authentication createAuthFromDetails(UserDetails user) {
        return AuthenticationData.build()
          .authorities(user.getAuthorities())
          .authenticated(true)
          .principal(user)
          .name(user.getUsername())
          .build();
    }

    private AuthenticationData createUserAuth(String ... roles) {
        List<GrantedAuthorityImpl> authorities = Arrays.stream(roles)
          .map((r) -> new GrantedAuthorityImpl(r, ROOT))
          .collect(Collectors.toList());
        return AuthenticationData.build()
          .principal(user)
          .authorities(authorities).build();
    }

    private void create() {
        CreateContainerCmd cmd = new CreateContainerCmd();
        cmd.setName("test");
        cmd.setImage("testimage");
        service.createContainer(cmd);
    }
}