package com.codeabovelab.dm.security.acl;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public class Utilities {
    
    public static AutoCloseable overrideAuth(UserDetailsService userDetailsService, String login) {
        final UserDetails globalAdmin = userDetailsService.loadUserByUsername(login);
        final SecurityContext context = SecurityContextHolder.getContext();
        final Authentication old = context.getAuthentication();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(globalAdmin, null, globalAdmin.getAuthorities()));
        return () -> context.setAuthentication(old);
    }
}
