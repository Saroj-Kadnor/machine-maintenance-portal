package com.saroj.machine_maintenance_portal.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.saroj.machine_maintenance_portal.model.Roles;

/** Small helpers for reading the logged-in user's role. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static String role(Authentication auth) {
        if (auth == null) {
            return "";
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String name = authority.getAuthority();
            if (name.startsWith("ROLE_")) {
                return name.substring(5);
            }
        }
        return "";
    }

    public static String username(Authentication auth) {
        return auth == null ? "" : auth.getName();
    }

    public static boolean canManage(Authentication auth) {
        String role = role(auth);
        return Roles.ADMIN.equals(role) || Roles.MANAGER.equals(role);
    }
}
