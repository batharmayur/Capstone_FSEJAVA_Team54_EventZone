package com.eventzone.backend.service;

import com.eventzone.backend.dto.EventRequest;
import com.eventzone.backend.dto.OrganiserEventResponse;
import com.eventzone.backend.dto.OrganiserTicketCategoryResponse;
import com.eventzone.backend.dto.TicketCategoryRequest;
import com.eventzone.backend.exception.BusinessRuleException;
import com.eventzone.backend.exception.ResourceNotFoundException;
import com.eventzone.backend.model.Event;
import com.eventzone.backend.model.EventCategory;
import com.eventzone.backend.model.Role;
import com.eventzone.backend.model.TicketCategory;
import com.eventzone.backend.model.User;
import com.eventzone.backend.repository.BookingRepository;
import com.eventzone.backend.repository.EventCategoryRepository;
import com.eventzone.backend.repository.EventRepository;
import com.eventzone.backend.repository.TicketCategoryRepository;
import com.eventzone.backend.repository.UserRepository;
import com.eventzone.backend.security.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventManagementServiceTest {

    private static final Actor OWNER = new Actor("org1@eventzone.com", false);
    private static final Actor OTHER_ORGANISER = new Actor("org2@eventzone.com", false);
    private static final Actor ADMIN = new Actor("admin@eventzone.com", true);

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventCategoryRepository categoryRepository;
    @Mock
    private TicketCategoryRepository ticketCategoryRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;

    private EventManagementService service;
    private User organiser;
    private EventCategory concert;
    private Event event;

    @BeforeEach
    void setUp() {
        service = new EventManagementService(eventRepository, categoryRepository, ticketCategoryRepository,
                bookingRepository, userRepository);
        organiser = new User(OWNER.email(), "hash", Role.ORGANISER, "Arjun Events");
        concert = new EventCategory("Concert");
        event = new Event("Rock Night", "desc", LocalDateTime.now().plusDays(10), "HICC", null, concert);
        event.setOrganiser(organiser);
    }

    private EventRequest request(String title) {
        return new EventRequest(" " + title + " ", "  ", LocalDateTime.now().plusDays(20), "Arena", "", UUID.randomUUID());
    }

    @Test
    void createEventAssignsTheSignedInOrganiserAndTrimsInput() {
        when(userRepository.findByEmail(OWNER.email())).thenReturn(Optional.of(organiser));
        when(categoryRepository.findById(any())).thenReturn(Optional.of(concert));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganiserEventResponse response = service.createEvent(OWNER.email(), request("New Show"));

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue().getOrganiser()).isSameAs(organiser);
        assertThat(response.title()).isEqualTo("New Show");
        assertThat(response.description()).isNull();
        assertThat(response.coverImageUrl()).isNull();
        assertThat(response.active()).isTrue();
        assertThat(response.ticketCategories()).isEmpty();
    }

    @Test
    void createEventRejectsUnknownCategory() {
        when(userRepository.findByEmail(OWNER.email())).thenReturn(Optional.of(organiser));
        when(categoryRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createEvent(OWNER.email(), request("X")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void ownerAndAdminCanUpdateButAnotherOrganiserCannot() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findWithDetailsById(id)).thenReturn(Optional.of(event));
        when(categoryRepository.findById(any())).thenReturn(Optional.of(concert));

        assertThat(service.updateEvent(OWNER, id, request("Renamed")).title()).isEqualTo("Renamed");
        assertThat(service.updateEvent(ADMIN, id, request("By Admin")).title()).isEqualTo("By Admin");
        assertThatThrownBy(() -> service.updateEvent(OTHER_ORGANISER, id, request("Hijacked")))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(event.getTitle()).isEqualTo("By Admin");
    }

    @Test
    void eventWithoutOrganiserCanOnlyBeManagedByAdmin() {
        UUID id = UUID.randomUUID();
        event.setOrganiser(null);
        when(eventRepository.findWithDetailsById(id)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.deleteEvent(OWNER, id)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteEventIsBlockedWhenBookingsExist() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findWithDetailsById(id)).thenReturn(Optional.of(event));
        when(bookingRepository.existsByTicketCategoryEventId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteEvent(OWNER, id)).isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).delete(any());
    }

    @Test
    void deleteEventRemovesItWhenThereAreNoBookings() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findWithDetailsById(id)).thenReturn(Optional.of(event));
        when(bookingRepository.existsByTicketCategoryEventId(id)).thenReturn(false);

        service.deleteEvent(OWNER, id);

        verify(eventRepository).delete(event);
    }

    @Test
    void addTicketCategoryIsRestrictedToTheEventOwner() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findWithDetailsById(id)).thenReturn(Optional.of(event));
        when(ticketCategoryRepository.save(any(TicketCategory.class))).thenAnswer(inv -> inv.getArgument(0));
        TicketCategoryRequest request = new TicketCategoryRequest(" VIP ", new BigDecimal("2499.00"), 50);

        OrganiserTicketCategoryResponse created = service.addTicketCategory(OWNER, id, request);

        assertThat(created.name()).isEqualTo("VIP");
        assertThat(created.totalSeats()).isEqualTo(50);
        assertThat(created.availableSeats()).isEqualTo(50);
        assertThat(created.bookedSeats()).isZero();
        assertThatThrownBy(() -> service.addTicketCategory(OTHER_ORGANISER, id, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateTicketCategoryRejectsTotalBelowBookedSeats() {
        TicketCategory ticket = new TicketCategory(event, "General", BigDecimal.TEN, 100);
        UUID id = UUID.randomUUID();
        when(ticketCategoryRepository.findWithEventById(id)).thenReturn(Optional.of(ticket));
        when(ticketCategoryRepository.updateDetails(any(), eq("General"), any(), eq(5))).thenReturn(0);
        when(ticketCategoryRepository.findAvailableSeats(any())).thenReturn(90);

        assertThatThrownBy(() -> service.updateTicketCategory(OWNER, id,
                new TicketCategoryRequest("General", BigDecimal.TEN, 5)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("10 seat(s) already booked");
    }

    @Test
    void updateTicketCategoryIsDeniedForNonOwnerWithoutTouchingSeats() {
        TicketCategory ticket = new TicketCategory(event, "General", BigDecimal.TEN, 100);
        UUID id = UUID.randomUUID();
        when(ticketCategoryRepository.findWithEventById(id)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.updateTicketCategory(OTHER_ORGANISER, id,
                new TicketCategoryRequest("General", BigDecimal.TEN, 200)))
                .isInstanceOf(AccessDeniedException.class);
        verify(ticketCategoryRepository, never()).updateDetails(any(), any(), any(), anyInt());
    }

    @Test
    void deleteTicketCategoryIsBlockedWhenBookingsExist() {
        TicketCategory ticket = new TicketCategory(event, "General", BigDecimal.TEN, 100);
        UUID id = UUID.randomUUID();
        when(ticketCategoryRepository.findWithEventById(id)).thenReturn(Optional.of(ticket));
        when(bookingRepository.existsByTicketCategoryId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteTicketCategory(OWNER, id)).isInstanceOf(BusinessRuleException.class);
        verify(ticketCategoryRepository, never()).delete(any());
    }

    @Test
    void myEventsReportsBookedSeatsAndConfirmedBookingCounts() {
        TicketCategory general = new TicketCategory(event, "General", BigDecimal.TEN, 100);
        event.getTicketCategories().add(general);
        when(eventRepository.findByOrganiserEmailOrderByEventDateAsc(OWNER.email())).thenReturn(List.of(event));
        when(bookingRepository.countConfirmedByTicketCategoryIds(any()))
                .thenReturn(List.<Object[]>of(new Object[]{general.getId(), 3L}));

        List<OrganiserEventResponse> events = service.myEvents(OWNER.email());

        assertThat(events).hasSize(1);
        assertThat(events.get(0).ticketCategories()).hasSize(1);
        assertThat(events.get(0).ticketCategories().get(0).bookingCount()).isEqualTo(3L);
        assertThat(events.get(0).ticketCategories().get(0).totalSeats()).isEqualTo(100);
    }
}
