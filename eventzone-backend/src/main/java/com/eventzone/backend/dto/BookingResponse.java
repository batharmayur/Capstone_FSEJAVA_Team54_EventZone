package com.eventzone.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        String bookingRef,
        UUID eventId,
        String eventTitle,
        LocalDateTime eventDate,
        String venue,
        String ticketCategory,
        int quantity,
        BigDecimal totalAmount,
        String status,
        Instant createdAt
) {
}
