package com.eventzone.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminEventResponse(
        UUID id,
        String title,
        String category,
        LocalDateTime eventDate,
        String venue,
        boolean active,
        String organiser
) {
}
