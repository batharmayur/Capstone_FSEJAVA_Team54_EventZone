package com.eventzone.backend.security;

import org.springframework.security.core.Authentication;

/** The signed-in caller, derived from the validated token. */
public record Actor(String email, boolean admin) {

    public static Actor from(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return new Actor(authentication.getName(), admin);
    }
}
