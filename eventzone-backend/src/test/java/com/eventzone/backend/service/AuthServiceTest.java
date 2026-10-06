package com.eventzone.backend.service;

import com.eventzone.backend.dto.AuthResponse;
import com.eventzone.backend.dto.LoginRequest;
import com.eventzone.backend.dto.RegisterRequest;
import com.eventzone.backend.dto.UserResponse;
import com.eventzone.backend.exception.AuthenticationFailedException;
import com.eventzone.backend.exception.DuplicateResourceException;
import com.eventzone.backend.model.Role;
import com.eventzone.backend.model.User;
import com.eventzone.backend.repository.UserRepository;
import com.eventzone.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService("unit-test-secret-0123456789-0123456789-0123", 5);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerHashesPasswordNormalisesEmailAndDefaultsToAttendee() {
        when(userRepository.existsByEmail("divya@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(new RegisterRequest("  Divya@Example.com ", "Secret@123", " Divya "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("Secret@123");
        assertThat(passwordEncoder.matches("Secret@123", saved.getValue().getPasswordHash())).isTrue();
        assertThat(response.email()).isEqualTo("divya@example.com");
        assertThat(response.name()).isEqualTo("Divya");
        assertThat(response.role()).isEqualTo(Role.ATTENDEE);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("divya@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("divya@example.com", "Secret@123", "Divya")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void loginReturnsParsableTokenForValidCredentials() {
        User user = new User("org@example.com", passwordEncoder.encode("Secret@123"), Role.ORGANISER, "Org");
        when(userRepository.findByEmail("org@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest("ORG@example.com", "Secret@123"));

        assertThat(response.user().role()).isEqualTo(Role.ORGANISER);
        assertThat(response.expiresAt()).isAfter(java.time.Instant.now());
        assertThat(jwtService.parse(response.token()))
                .hasValueSatisfying(p -> {
                    assertThat(p.email()).isEqualTo("org@example.com");
                    assertThat(p.role()).isEqualTo(Role.ORGANISER);
                });
    }

    @Test
    void loginFailsWithSameMessageForWrongPasswordAndUnknownUser() {
        User user = new User("org@example.com", passwordEncoder.encode("Secret@123"), Role.ORGANISER, "Org");
        when(userRepository.findByEmail("org@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("org@example.com", "wrong")))
                .isInstanceOf(AuthenticationFailedException.class).hasMessage("Invalid email or password");
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "whatever")))
                .isInstanceOf(AuthenticationFailedException.class).hasMessage("Invalid email or password");
    }
}
