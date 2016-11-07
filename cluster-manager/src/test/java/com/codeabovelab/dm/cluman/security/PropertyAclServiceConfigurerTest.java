package com.codeabovelab.dm.cluman.security;

import com.codeabovelab.dm.common.security.Action;
import com.codeabovelab.dm.common.security.TenantGrantedAuthoritySid;
import com.codeabovelab.dm.common.security.TenantPrincipalSid;
import com.codeabovelab.dm.common.security.acl.AceSource;
import com.codeabovelab.dm.common.security.acl.AclSource;
import com.codeabovelab.dm.common.security.acl.AclUtils;
import com.codeabovelab.dm.common.security.dto.PermissionData;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

/**
 */
@Slf4j
@ActiveProfiles("test_acl")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PropertyAclServiceConfigurerTest.AppConfiguration.class)
public class PropertyAclServiceConfigurerTest {

    @Configuration
    @EnableAutoConfiguration(exclude = EndpointWebMvcAutoConfiguration.class)
    @EnableConfigurationProperties(PropertyAclServiceConfigurer.class)
    public static class AppConfiguration {
    }

    @Autowired
    private PropertyAclServiceConfigurer configurer;

    @Autowired
    private Environment environment;

    @Test
    public void test() {
        assertNotNull("Configurer not wired", configurer);
        Map<String, String> list = configurer.getList();
        assertNotNull("Configurer has null list", list);
        assertThat("Configurer has no data", list.entrySet(), not(empty()));

        ConfigurableAclService.Builder b = ConfigurableAclService.builder();
        configurer.configure(b);

        Map<String, AclSource> acls = b.getAcls();
        assertNotNull("No acl for container type", acls.get(AclUtils.toId(SecuredType.CONTAINER.typeId())));
        AclSource syscont = acls.get(AclUtils.toId(SecuredType.CONTAINER.id("syscont")));
        assertNotNull("No acl for syscont container", syscont);
        assertEquals(new TenantPrincipalSid("user", "root"), syscont.getOwner());
        List<AceSource> entries = syscont.getEntries();
        assertThat(entries, hasSize(2));
        AceSource ace = entries.get(1);
        assertEquals(false, ace.isGranting());
        assertEquals(PermissionData.from(Action.DELETE), ace.getPermission());
        assertEquals(new TenantGrantedAuthoritySid("ROLE_USER", "root"), ace.getSid());
    }
}