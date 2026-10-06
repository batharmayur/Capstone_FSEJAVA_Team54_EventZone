package com.eventzone.backend.controller;

import com.eventzone.backend.dto.EventRequest;
import com.eventzone.backend.dto.OrganiserEventResponse;
import com.eventzone.backend.dto.OrganiserTicketCategoryResponse;
import com.eventzone.backend.dto.TicketCategoryRequest;
import com.eventzone.backend.security.Actor;
import com.eventzone.backend.service.EventManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Role rules live in SecurityConfig; ownership of each event is enforced in the service. */
@RestController
@RequestMapping("/api")
@Tag(name = "Organiser", description = "Manage events and ticket categories (ORGANISER; admins may edit or delete any event)")
public class EventManagementController {

    private final EventManagementService service;

    public EventManagementController(EventManagementService service) {
        this.service = service;
    }

    @GetMapping("/organiser/events")
    @Operation(summary = "My events with booking counts per ticket category")
    public List<OrganiserEventResponse> myEvents(Authentication authentication) {
        return service.myEvents(authentication.getName());
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an event (appears in the catalog immediately)")
    public OrganiserEventResponse createEvent(@Valid @RequestBody EventRequest request, Authentication authentication) {
        return service.createEvent(authentication.getName(), request);
    }

    @PutMapping("/events/{id}")
    @Operation(summary = "Update an event (owner or admin)")
    public OrganiserEventResponse updateEvent(@PathVariable UUID id, @Valid @RequestBody EventRequest request,
                                              Authentication authentication) {
        return service.updateEvent(Actor.from(authentication), id, request);
    }

    @DeleteMapping("/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an event without bookings (owner or admin)")
    public void deleteEvent(@PathVariable UUID id, Authentication authentication) {
        service.deleteEvent(Actor.from(authentication), id);
    }

    @PostMapping("/events/{id}/ticket-categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a ticket category to my event")
    public OrganiserTicketCategoryResponse addTicketCategory(@PathVariable UUID id,
                                                             @Valid @RequestBody TicketCategoryRequest request,
                                                             Authentication authentication) {
        return service.addTicketCategory(Actor.from(authentication), id, request);
    }

    @PutMapping("/ticket-categories/{id}")
    @Operation(summary = "Update a ticket category (total cannot drop below seats already booked)")
    public OrganiserTicketCategoryResponse updateTicketCategory(@PathVariable UUID id,
                                                                @Valid @RequestBody TicketCategoryRequest request,
                                                                Authentication authentication) {
        return service.updateTicketCategory(Actor.from(authentication), id, request);
    }

    @DeleteMapping("/ticket-categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a ticket category without bookings")
    public void deleteTicketCategory(@PathVariable UUID id, Authentication authentication) {
        service.deleteTicketCategory(Actor.from(authentication), id);
    }
}
