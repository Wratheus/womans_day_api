package com.womansday.api.security;

import com.womansday.api.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long extractUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    public static Role extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .map(r -> {
                    try { return Role.valueOf(r); }
                    catch (IllegalArgumentException e) { return Role.USER; }
                })
                .orElse(Role.USER);
    }

    public static String extractRoleString(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse(Role.USER.name());
    }
}
