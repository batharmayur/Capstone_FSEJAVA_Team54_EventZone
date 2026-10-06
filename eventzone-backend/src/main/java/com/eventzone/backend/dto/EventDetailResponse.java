package com.eventzone.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EventDetailResponse(
        UUID id,
        String title,
        String description,
        String category,
        LocalDateTime eventDate,
        String venue,
        String coverImageUrl,
        List<TicketCategoryResponse> ticketCategories
) {
}
