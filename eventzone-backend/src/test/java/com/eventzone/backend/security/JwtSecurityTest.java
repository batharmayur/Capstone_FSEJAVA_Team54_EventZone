package com.eventzone.backend.security;

import com.eventzone.backend.model.Role;
import com.eventzone.backend.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecurityTest {

    private static final String SECRET = "unit-test-secret-0123456789-0123456789-0123";

    private final JwtService jwtService = new JwtService(SECRET, 5);
    private final User user = new User("a@example.com", "hash", Role.ADMIN, "A");

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 5)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void parseRejectsTamperedExpiredAndForeignSignedTokens() {
        String token = jwtService.generateToken(user).token();

        assertThat(jwtService.parse(token + "x")).isEmpty();
        assertThat(jwtService.parse("not-a-jwt")).isEmpty();
        assertThat(new JwtService(SECRET, -1).parse(new JwtService(SECRET, -1).generateToken(user).token())).isEmpty();
        assertThat(new JwtService("another-secret-0123456789-0123456789-01", 5).parse(token)).isEmpty();
    }

    @Test
    void filterAuthenticatesRequestWithValidBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken(user).token());

        new JwtAuthenticationFilter(jwtService).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("a@example.com");
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    void filterLeavesRequestUnauthenticatedForMissingOrInvalidToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        MockHttpServletRequest bad = new MockHttpServletRequest();
        bad.addHeader("Authorization", "Bearer garbage");
        filter.doFilter(bad, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
