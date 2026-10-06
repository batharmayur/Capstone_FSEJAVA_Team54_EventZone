package com.eventzone.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrganiserEventResponse(
        UUID id,
        String title,
        String description,
        UUID categoryId,
        String category,
        LocalDateTime eventDate,
        String venue,
        String coverImageUrl,
        boolean active,
        List<OrganiserTicketCategoryResponse> ticketCategories
) {
}
