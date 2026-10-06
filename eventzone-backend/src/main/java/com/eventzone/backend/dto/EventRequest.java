package com.eventzone.backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull @Future LocalDateTime eventDate,
        @NotBlank @Size(max = 200) String venue,
        @Size(max = 500)
        @Pattern(regexp = "^(https?://|/)\\S*$", message = "must be an http(s) URL or a site-relative path")
        String coverImageUrl,
        @NotNull UUID categoryId
) {
}
