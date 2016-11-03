package com.codeabovelab.dm.security.acl;

import com.codeabovelab.dm.common.security.Authorities;
import com.codeabovelab.dm.common.security.MultiTenancySupport;
import com.codeabovelab.dm.security.entity.Authority;
import com.codeabovelab.dm.security.entity.TenantEntity;
import com.codeabovelab.dm.security.entity.UserAuthDetails;
import com.codeabovelab.dm.security.repository.AuthorityRepository;
import com.codeabovelab.dm.security.repository.TenantsRepository;
import com.codeabovelab.dm.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.*;

public class SampleUserDetailsService implements UserDetailsService {
    public static final String USER_THIRD_TENANT_ONE = "third_tenant_two";
    public static final String USER_SECOND_TENANT_TWO = "second_tenant_one";
    public static final String USER_ONE_TENANT_TWO = "one_tenant_one";
    public static final String USER_GLOBAL = "global";

    @Autowired
    @Lazy
    private TenantsRepository tenantsRepository;

    @Autowired
    @Lazy
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private AuthorityRepository authorityRepository;
    private TenantEntity tenantRoot;
    private TenantEntity tenantOne;
    private TenantEntity tenantTwo;

    public SampleUserDetailsService() {

    }

    private UserAuthDetails newUser(String userLogin, Set<Authority> authorities, String tenantId) {
        UserAuthDetails details = TestObjectsFactory.createUser(userLogin);
        details.setAuthorities(authorities);
        TenantEntity tenant = getTenant(tenantId);
        details.setTenantEntity(tenant);
        return details;
    }

    private Authority authority(String role, String tenantId) {
        Authority authority = new Authority();
        authority.setRole(role);
        TenantEntity tenant = getTenant(tenantId);
        authority.setTenantEntity(tenant);
        return authority;
    }

    private TenantEntity getTenant(String tenantId) {
        return tenantId == MultiTenancySupport.NO_TENANT? null : tenantsRepository.findByName(tenantId);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        loadIfNeed();
        UserDetails user = userRepository.findByUsername(username);
        if (user == null) {

            throw new UsernameNotFoundException(username);
        }
        return user;
    }

    private void loadIfNeed() {
        if(userRepository.findByUsername(USER_GLOBAL) != null) {
            return;
        }
        saveTenants(tenantsRepository);
        Authority userRole = authority(Authorities.USER_ROLE, MultiTenancySupport.NO_TENANT);
        Set<Authority> defaultRoles = Collections.singleton(userRole);
        Authority globalAdminRole = authority(Authorities.ADMIN_ROLE, this.tenantRoot.getName());
        Set<Authority> globalAdminRoles = new HashSet<>();
        globalAdminRoles.addAll(defaultRoles);
        globalAdminRoles.add(globalAdminRole);
        add(newUser(USER_ONE_TENANT_TWO, defaultRoles, this.tenantTwo.getName()));
        add(newUser(USER_SECOND_TENANT_TWO, defaultRoles, this.tenantTwo.getName()));
        add(newUser(USER_THIRD_TENANT_ONE, defaultRoles, this.tenantOne.getName()));
        add(newUser(USER_GLOBAL, Collections.unmodifiableSet(globalAdminRoles), this.tenantRoot.getName()));
        userRepository.flush();
    }

    private void add(UserAuthDetails customUser) {
        authorityRepository.save(customUser.getAuthorities());
        userRepository.saveAndFlush(customUser);
    }


    /**
     * save default tenants hierarchy into repository
     * @param tenantsRepository
     */
    void saveTenants(TenantsRepository tenantsRepository) {
        TenantEntity rt = new TenantEntity().name("root_tenant").root(true);
        rt = tenantsRepository.save(rt);
        this.tenantRoot = tenantsRepository.findOne(rt.getId());
        TenantEntity tenantOne = tenantsRepository.save(new TenantEntity());
        this.tenantTwo = tenantsRepository.save(new TenantEntity().parent(tenantOne));
        tenantOne.setChildren(new ArrayList<>(Arrays.asList(tenantTwo)));
        this.tenantOne = tenantOne = tenantsRepository.save(tenantOne);
        tenantsRepository.flush();
    }
}
