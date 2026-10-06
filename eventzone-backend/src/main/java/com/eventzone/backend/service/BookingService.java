package com.eventzone.backend.service;

import com.eventzone.backend.dto.BookingRequest;
import com.eventzone.backend.dto.BookingResponse;
import com.eventzone.backend.exception.AuthenticationFailedException;
import com.eventzone.backend.exception.BusinessRuleException;
import com.eventzone.backend.exception.ResourceNotFoundException;
import com.eventzone.backend.model.Booking;
import com.eventzone.backend.model.Event;
import com.eventzone.backend.model.TicketCategory;
import com.eventzone.backend.model.User;
import com.eventzone.backend.repository.BookingRepository;
import com.eventzone.backend.repository.TicketCategoryRepository;
import com.eventzone.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final String REF_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int REF_LENGTH = 8;

    private final BookingRepository bookingRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public BookingService(BookingRepository bookingRepository, TicketCategoryRepository ticketCategoryRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createBooking(String email, BookingRequest request) {
        User user = currentUser(email);
        TicketCategory ticket = ticketCategoryRepository.findWithEventById(request.ticketCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket category not found: " + request.ticketCategoryId()));

        Event event = ticket.getEvent();
        if (!event.isActive() || !event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("This event is not open for booking");
        }

        int quantity = request.quantity();
        if (ticketCategoryRepository.reserveSeats(ticket.getId(), quantity) == 0) {
            int left = ticketCategoryRepository.findAvailableSeats(ticket.getId());
            throw new BusinessRuleException(left == 0 ? "This ticket category is sold out"
                    : "Only " + left + " seat(s) left in this ticket category");
        }

        BigDecimal total = ticket.getPrice().multiply(BigDecimal.valueOf(quantity));
        Booking saved = bookingRepository.save(new Booking(user, ticket, quantity, total, newBookingRef()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(String email) {
        return bookingRepository.findByUserEmailOrderByCreatedAtDesc(email).stream()
                .map(BookingService::toResponse)
                .toList();
    }

    /** Only the owner can cancel. Someone else's booking is reported as not found so ids are not leaked. */
    @Transactional
    public BookingResponse cancel(String email, UUID bookingId) {
        Booking booking = bookingRepository.findForUpdateByIdAndUserEmail(bookingId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.isCancelled()) {
            throw new BusinessRuleException("This booking is already cancelled");
        }

        booking.cancel();
        ticketCategoryRepository.releaseSeats(booking.getTicketCategory().getId(), booking.getQuantity());
        return toResponse(booking);
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("Account no longer exists"));
    }

    private String newBookingRef() {
        StringBuilder ref = new StringBuilder("EZ-");
        for (int i = 0; i < REF_LENGTH; i++) {
            ref.append(REF_ALPHABET.charAt(random.nextInt(REF_ALPHABET.length())));
        }
        return ref.toString();
    }

    private static BookingResponse toResponse(Booking booking) {
        TicketCategory ticket = booking.getTicketCategory();
        Event event = ticket.getEvent();
        return new BookingResponse(
                booking.getId(),
                booking.getBookingRef(),
                event.getId(),
                event.getTitle(),
                event.getEventDate(),
                event.getVenue(),
                ticket.getName(),
                booking.getQuantity(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
