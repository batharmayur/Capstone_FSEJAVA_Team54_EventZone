package com.eventzone.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventSummaryResponse(
        UUID id,
        String title,
        String category,
        LocalDateTime eventDate,
        String venue,
        String coverImageUrl,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
