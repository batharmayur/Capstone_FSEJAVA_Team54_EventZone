package com.eventzone.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Ticket category with booking figures: bookedSeats = seats taken, bookingCount = confirmed bookings. */
public record OrganiserTicketCategoryResponse(
        UUID id,
        String name,
        BigDecimal price,
        int totalSeats,
        int availableSeats,
        int bookedSeats,
        long bookingCount
) {
}
