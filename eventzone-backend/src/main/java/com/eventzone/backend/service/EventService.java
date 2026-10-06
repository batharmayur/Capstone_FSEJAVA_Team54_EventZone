package com.eventzone.backend.service;

import com.eventzone.backend.dto.EventCategoryResponse;
import com.eventzone.backend.dto.EventDetailResponse;
import com.eventzone.backend.dto.EventSummaryResponse;
import com.eventzone.backend.dto.TicketCategoryResponse;
import com.eventzone.backend.exception.ResourceNotFoundException;
import com.eventzone.backend.model.Event;
import com.eventzone.backend.model.TicketCategory;
import com.eventzone.backend.repository.EventCategoryRepository;
import com.eventzone.backend.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;

    public EventService(EventRepository eventRepository, EventCategoryRepository categoryRepository) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<EventSummaryResponse> listEvents(String category) {
        List<Event> events = (category == null || category.isBlank())
                ? eventRepository.findByActiveTrueOrderByEventDateAsc()
                : eventRepository.findByActiveTrueAndCategoryNameIgnoreCaseOrderByEventDateAsc(category.trim());
        return events.stream().map(this::toSummary).toList();
    }

    public List<EventCategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(c -> new EventCategoryResponse(c.getId(), c.getName()))
                .toList();
    }

    public EventDetailResponse getEvent(UUID id) {
        Event event = eventRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));

        List<TicketCategoryResponse> tickets = event.getTicketCategories().stream()
                .sorted(Comparator.comparing(TicketCategory::getPrice))
                .map(t -> new TicketCategoryResponse(
                        t.getId(), t.getName(), t.getPrice(), t.getTotalSeats(), t.getAvailableSeats()))
                .toList();

        return new EventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory().getName(),
                event.getEventDate(),
                event.getVenue(),
                event.getCoverImageUrl(),
                tickets
        );
    }

    private EventSummaryResponse toSummary(Event event) {
        List<BigDecimal> prices = event.getTicketCategories().stream()
                .map(TicketCategory::getPrice)
                .toList();
        BigDecimal min = prices.stream().min(Comparator.naturalOrder()).orElse(null);
        BigDecimal max = prices.stream().max(Comparator.naturalOrder()).orElse(null);

        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getCategory().getName(),
                event.getEventDate(),
                event.getVenue(),
                event.getCoverImageUrl(),
                min,
                max
        );
    }
}
