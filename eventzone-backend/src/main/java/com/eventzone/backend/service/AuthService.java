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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** New accounts are always ATTENDEE; organisers are seeded or promoted by an admin. */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalise(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        try {
            User saved = userRepository.saveAndFlush(new User(
                    email, passwordEncoder.encode(request.password()), Role.ATTENDEE, request.name().trim()));
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalise(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS));

        JwtService.IssuedToken issued = jwtService.generateToken(user);
        return new AuthResponse(issued.token(), issued.expiresAt(), toResponse(user));
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
