package com.eventzone.backend.service;

import com.eventzone.backend.dto.BookingRequest;
import com.eventzone.backend.dto.BookingResponse;
import com.eventzone.backend.exception.BusinessRuleException;
import com.eventzone.backend.exception.ResourceNotFoundException;
import com.eventzone.backend.model.Booking;
import com.eventzone.backend.model.BookingStatus;
import com.eventzone.backend.model.Event;
import com.eventzone.backend.model.EventCategory;
import com.eventzone.backend.model.Role;
import com.eventzone.backend.model.TicketCategory;
import com.eventzone.backend.model.User;
import com.eventzone.backend.repository.BookingRepository;
import com.eventzone.backend.repository.TicketCategoryRepository;
import com.eventzone.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final String EMAIL = "user1@eventzone.com";

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TicketCategoryRepository ticketCategoryRepository;
    @Mock
    private UserRepository userRepository;

    private BookingService bookingService;
    private User user;
    private Event event;
    private TicketCategory ticket;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, ticketCategoryRepository, userRepository);
        user = new User(EMAIL, "hash", Role.ATTENDEE, "Divya");
        event = new Event("Rock Night", "desc", LocalDateTime.now().plusDays(10), "HICC", null,
                new EventCategory("Concert"));
        ticket = new TicketCategory(event, "General", new BigDecimal("999.00"), 200);
    }

    private void stubUserAndTicket() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(ticketCategoryRepository.findWithEventById(any())).thenReturn(Optional.of(ticket));
    }

    @Test
    void createBookingReservesSeatsAndReturnsConfirmedBookingWithTotal() {
        stubUserAndTicket();
        when(ticketCategoryRepository.reserveSeats(any(), anyInt())).thenReturn(1);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = bookingService.createBooking(EMAIL, new BookingRequest(UUID.randomUUID(), 2));

        verify(ticketCategoryRepository).reserveSeats(ticket.getId(), 2);
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.totalAmount()).isEqualByComparingTo("1998.00");
        assertThat(response.bookingRef()).matches("EZ-[A-Z2-9]{8}");
        assertThat(response.eventTitle()).isEqualTo("Rock Night");
    }

    @Test
    void createBookingFailsWhenNotEnoughSeatsAndSavesNothing() {
        stubUserAndTicket();
        when(ticketCategoryRepository.reserveSeats(any(), anyInt())).thenReturn(0);
        when(ticketCategoryRepository.findAvailableSeats(any())).thenReturn(1);

        assertThatThrownBy(() -> bookingService.createBooking(EMAIL, new BookingRequest(UUID.randomUUID(), 3)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only 1 seat");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingReportsSoldOut() {
        stubUserAndTicket();
        when(ticketCategoryRepository.reserveSeats(any(), anyInt())).thenReturn(0);
        when(ticketCategoryRepository.findAvailableSeats(any())).thenReturn(0);

        assertThatThrownBy(() -> bookingService.createBooking(EMAIL, new BookingRequest(UUID.randomUUID(), 1)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("sold out");
    }

    @Test
    void createBookingRejectsInactiveOrPastEventsWithoutTouchingSeats() {
        stubUserAndTicket();
        event.setActive(false);
        assertThatThrownBy(() -> bookingService.createBooking(EMAIL, new BookingRequest(UUID.randomUUID(), 1)))
                .isInstanceOf(BusinessRuleException.class);

        event.setActive(true);
        Event past = new Event("Old", "d", LocalDateTime.now().minusDays(1), "V", null, new EventCategory("Concert"));
        when(ticketCategoryRepository.findWithEventById(any()))
                .thenReturn(Optional.of(new TicketCategory(past, "General", BigDecimal.TEN, 10)));
        assertThatThrownBy(() -> bookingService.createBooking(EMAIL, new BookingRequest(UUID.randomUUID(), 1)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketCategoryRepository, never()).reserveSeats(any(), anyInt());
    }

    @Test
    void createBookingRejectsUnknownTicketCategory() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(ticketCategoryRepository.findWithEventById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(EMAIL, new BookingRequest(UUID.randomUUID(), 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelMarksCancelledAndRestoresExactlyTheBookedQuantity() {
        Booking booking = new Booking(user, ticket, 2, new BigDecimal("1998.00"), "EZ-ABCDEFGH");
        UUID id = UUID.randomUUID();
        when(bookingRepository.findForUpdateByIdAndUserEmail(id, EMAIL)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.cancel(EMAIL, id);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        ArgumentCaptor<Integer> qty = ArgumentCaptor.forClass(Integer.class);
        verify(ticketCategoryRepository).releaseSeats(any(), qty.capture());
        assertThat(qty.getValue()).isEqualTo(2);
    }

    @Test
    void cancelTwiceIsRejectedAndDoesNotRestoreSeatsAgain() {
        Booking booking = new Booking(user, ticket, 2, BigDecimal.TEN, "EZ-ABCDEFGH");
        booking.cancel();
        UUID id = UUID.randomUUID();
        when(bookingRepository.findForUpdateByIdAndUserEmail(id, EMAIL)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(EMAIL, id)).isInstanceOf(BusinessRuleException.class);
        verify(ticketCategoryRepository, never()).releaseSeats(any(), anyInt());
    }

    @Test
    void cancelOfAnotherUsersOrUnknownBookingIsNotFound() {
        UUID id = UUID.randomUUID();
        when(bookingRepository.findForUpdateByIdAndUserEmail(id, "other@eventzone.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancel("other@eventzone.com", id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ticketCategoryRepository, never()).releaseSeats(any(), anyInt());
        verify(userRepository, never()).findByEmail(anyString());
    }
}
