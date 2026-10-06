package com.eventzone.backend.controller;

import com.eventzone.backend.dto.EventCategoryResponse;
import com.eventzone.backend.dto.EventDetailResponse;
import com.eventzone.backend.dto.EventSummaryResponse;
import com.eventzone.backend.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Events", description = "Public event catalog")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/events")
    @Operation(summary = "List active events, optionally filtered by category name")
    public List<EventSummaryResponse> listEvents(@RequestParam(required = false) String category) {
        return eventService.listEvents(category);
    }

    @GetMapping("/events/{id}")
    @Operation(summary = "Event detail with ticket categories and seats available")
    public EventDetailResponse getEvent(@PathVariable UUID id) {
        return eventService.getEvent(id);
    }

    @GetMapping("/categories")
    @Operation(summary = "List event categories")
    public List<EventCategoryResponse> listCategories() {
        return eventService.listCategories();
    }
}
