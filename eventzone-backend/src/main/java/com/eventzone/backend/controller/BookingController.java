package com.eventzone.backend.controller;

import com.eventzone.backend.dto.BookingRequest;
import com.eventzone.backend.dto.BookingResponse;
import com.eventzone.backend.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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

/** All endpoints require a signed-in user; the user is taken from the token, never from the request. */
@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Book, list and cancel tickets (signed-in users)")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book 1-5 tickets; deducts seats and returns a booking reference")
    public BookingResponse create(@Valid @RequestBody BookingRequest request, Authentication authentication) {
        return bookingService.createBooking(authentication.getName(), request);
    }

    @GetMapping("/mine")
    @Operation(summary = "List my bookings, newest first")
    public List<BookingResponse> mine(Authentication authentication) {
        return bookingService.myBookings(authentication.getName());
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel one of my bookings and restore its seats")
    public BookingResponse cancel(@PathVariable UUID id, Authentication authentication) {
        return bookingService.cancel(authentication.getName(), id);
    }
}
