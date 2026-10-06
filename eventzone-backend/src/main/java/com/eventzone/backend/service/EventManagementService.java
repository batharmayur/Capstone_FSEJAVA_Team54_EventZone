package com.eventzone.backend.service;

import com.eventzone.backend.dto.EventRequest;
import com.eventzone.backend.dto.OrganiserEventResponse;
import com.eventzone.backend.dto.OrganiserTicketCategoryResponse;
import com.eventzone.backend.dto.TicketCategoryRequest;
import com.eventzone.backend.exception.AuthenticationFailedException;
import com.eventzone.backend.exception.BusinessRuleException;
import com.eventzone.backend.exception.ResourceNotFoundException;
import com.eventzone.backend.model.Event;
import com.eventzone.backend.model.EventCategory;
import com.eventzone.backend.model.TicketCategory;
import com.eventzone.backend.model.User;
import com.eventzone.backend.repository.BookingRepository;
import com.eventzone.backend.repository.EventCategoryRepository;
import com.eventzone.backend.repository.EventRepository;
import com.eventzone.backend.repository.TicketCategoryRepository;
import com.eventzone.backend.repository.UserRepository;
import com.eventzone.backend.security.Actor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Organiser operations on events and their ticket categories. Admins may also manage any event. */
@Service
public class EventManagementService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public EventManagementService(EventRepository eventRepository, EventCategoryRepository categoryRepository,
                                  TicketCategoryRepository ticketCategoryRepository,
                                  BookingRepository bookingRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganiserEventResponse> myEvents(String email) {
        List<Event> events = eventRepository.findByOrganiserEmailOrderByEventDateAsc(email);
        Map<UUID, Long> counts = confirmedBookingCounts(events.stream()
                .flatMap(e -> e.getTicketCategories().stream()).map(TicketCategory::getId).toList());
        return events.stream().map(e -> toResponse(e, counts)).toList();
    }

    @Transactional
    public OrganiserEventResponse createEvent(String email, EventRequest request) {
        User organiser = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("Account no longer exists"));
        Event event = new Event(request.title().trim(), blankToNull(request.description()), request.eventDate(),
                request.venue().trim(), blankToNull(request.coverImageUrl()), findCategory(request.categoryId()));
        event.setOrganiser(organiser);
        return toResponse(eventRepository.save(event), Map.of());
    }

    @Transactional
    public OrganiserEventResponse updateEvent(Actor actor, UUID eventId, EventRequest request) {
        Event event = loadManageable(actor, eventId);
        event.update(request.title().trim(), blankToNull(request.description()), request.eventDate(),
                request.venue().trim(), blankToNull(request.coverImageUrl()), findCategory(request.categoryId()));
        return toResponse(event, confirmedBookingCounts(ticketIds(event)));
    }

    /** Events that have bookings cannot be deleted (history must be kept); deactivate them instead. */
    @Transactional
    public void deleteEvent(Actor actor, UUID eventId) {
        Event event = loadManageable(actor, eventId);
        if (bookingRepository.existsByTicketCategoryEventId(eventId)) {
            throw new BusinessRuleException("This event has bookings and cannot be deleted; ask an admin to deactivate it");
        }
        try {
            eventRepository.delete(event);
            eventRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleException("This event has bookings and cannot be deleted");
        }
    }

    @Transactional
    public OrganiserTicketCategoryResponse addTicketCategory(Actor actor, UUID eventId, TicketCategoryRequest request) {
        Event event = loadManageable(actor, eventId);
        TicketCategory saved = ticketCategoryRepository.save(
                new TicketCategory(event, request.name().trim(), request.price(), request.totalSeats()));
        return toResponse(saved, 0);
    }

    @Transactional
    public OrganiserTicketCategoryResponse updateTicketCategory(Actor actor, UUID ticketId,
                                                                TicketCategoryRequest request) {
        TicketCategory ticket = loadManageableTicket(actor, ticketId);
        int updated = ticketCategoryRepository.updateDetails(
                ticket.getId(), request.name().trim(), request.price(), request.totalSeats());
        if (updated == 0) {
            int booked = ticket.getTotalSeats() - ticketCategoryRepository.findAvailableSeats(ticket.getId());
            throw new BusinessRuleException("Total seats cannot be below the " + booked + " seat(s) already booked");
        }
        TicketCategory reloaded = ticketCategoryRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket category not found: " + ticketId));
        return toResponse(reloaded, confirmedBookingCounts(List.of(ticketId)).getOrDefault(ticketId, 0L));
    }

    @Transactional
    public void deleteTicketCategory(Actor actor, UUID ticketId) {
        TicketCategory ticket = loadManageableTicket(actor, ticketId);
        if (bookingRepository.existsByTicketCategoryId(ticketId)) {
            throw new BusinessRuleException("This ticket category has bookings and cannot be deleted");
        }
        try {
            ticketCategoryRepository.delete(ticket);
            ticketCategoryRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleException("This ticket category has bookings and cannot be deleted");
        }
    }

    private Event loadManageable(Actor actor, UUID eventId) {
        Event event = eventRepository.findWithDetailsById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        assertCanManage(event, actor);
        return event;
    }

    private TicketCategory loadManageableTicket(Actor actor, UUID ticketId) {
        TicketCategory ticket = ticketCategoryRepository.findWithEventById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket category not found: " + ticketId));
        assertCanManage(ticket.getEvent(), actor);
        return ticket;
    }

    private static void assertCanManage(Event event, Actor actor) {
        boolean owner = event.getOrganiser() != null && event.getOrganiser().getEmail().equals(actor.email());
        if (!owner && !actor.admin()) {
            throw new AccessDeniedException("You can only manage your own events");
        }
    }

    private EventCategory findCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    private Map<UUID, Long> confirmedBookingCounts(Collection<UUID> ticketIds) {
        Map<UUID, Long> counts = new HashMap<>();
        if (ticketIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : bookingRepository.countConfirmedByTicketCategoryIds(ticketIds)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }

    private static List<UUID> ticketIds(Event event) {
        return event.getTicketCategories().stream().map(TicketCategory::getId).toList();
    }

    private static OrganiserEventResponse toResponse(Event event, Map<UUID, Long> counts) {
        List<OrganiserTicketCategoryResponse> tickets = event.getTicketCategories().stream()
                .sorted(Comparator.comparing(TicketCategory::getPrice))
                .map(t -> toResponse(t, counts.getOrDefault(t.getId(), 0L)))
                .toList();
        return new OrganiserEventResponse(event.getId(), event.getTitle(), event.getDescription(),
                event.getCategory().getId(), event.getCategory().getName(), event.getEventDate(), event.getVenue(),
                event.getCoverImageUrl(), event.isActive(), tickets);
    }

    private static OrganiserTicketCategoryResponse toResponse(TicketCategory ticket, long bookingCount) {
        return new OrganiserTicketCategoryResponse(ticket.getId(), ticket.getName(), ticket.getPrice(),
                ticket.getTotalSeats(), ticket.getAvailableSeats(),
                ticket.getTotalSeats() - ticket.getAvailableSeats(), bookingCount);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
