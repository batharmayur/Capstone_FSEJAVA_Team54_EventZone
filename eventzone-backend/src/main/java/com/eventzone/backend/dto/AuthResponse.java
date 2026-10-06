package com.eventzone.backend.dto;

import java.time.Instant;

public record AuthResponse(String token, Instant expiresAt, UserResponse user) {
}
