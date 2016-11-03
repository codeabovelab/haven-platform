package com.codeabovelab.dm.security;

import com.codeabovelab.dm.security.acl.AclConfiguration;
import com.codeabovelab.dm.security.acl.SampleUserDetailsService;
import com.codeabovelab.dm.security.repository.UserRepository;
import com.codeabovelab.dm.security.sampleobject.SampleObjectsConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.method.P;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.parameters.AnnotationParameterNameDiscoverer;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
@EnableAutoConfiguration
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@ComponentScan(lazyInit = true, basePackageClasses = {/*TestsConfiguration.class,*/
        UserRepository.class, AclConfiguration.class})
public class TestsConfiguration {
    @Bean
    UserDetailsService createUserDetailsService() {
        return new SampleUserDetailsService();
    }

    @Bean
    MethodSecurityExpressionHandler expressionHandler(PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
        nameDiscoverer.addDiscoverer(new AnnotationParameterNameDiscoverer(P.class.getName()));
        expressionHandler.setParameterNameDiscoverer(nameDiscoverer);
        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        return expressionHandler;
    }
}
