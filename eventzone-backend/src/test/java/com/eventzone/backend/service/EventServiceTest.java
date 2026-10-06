package com.eventzone.backend.service;

import com.eventzone.backend.dto.EventDetailResponse;
import com.eventzone.backend.dto.EventSummaryResponse;
import com.eventzone.backend.dto.TicketCategoryResponse;
import com.eventzone.backend.exception.ResourceNotFoundException;
import com.eventzone.backend.model.Event;
import com.eventzone.backend.model.EventCategory;
import com.eventzone.backend.model.TicketCategory;
import com.eventzone.backend.repository.EventCategoryRepository;
import com.eventzone.backend.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventCategoryRepository categoryRepository;

    @InjectMocks
    private EventService eventService;

    private Event eventWithTickets(String title, EventCategory category, String... prices) {
        Event event = new Event(title, "desc", LocalDateTime.now().plusDays(5), "Venue", "http://img", category);
        for (String price : prices) {
            event.getTicketCategories().add(new TicketCategory(event, "T" + price, new BigDecimal(price), 10));
        }
        return event;
    }

    @Test
    void listEventsWithoutCategoryReturnsAllWithPriceRange() {
        Event event = eventWithTickets("Rock Night", new EventCategory("Concert"), "999", "2499");
        when(eventRepository.findByActiveTrueOrderByEventDateAsc()).thenReturn(List.of(event));

        List<EventSummaryResponse> result = eventService.listEvents(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Rock Night");
        assertThat(result.get(0).category()).isEqualTo("Concert");
        assertThat(result.get(0).minPrice()).isEqualByComparingTo("999");
        assertThat(result.get(0).maxPrice()).isEqualByComparingTo("2499");
    }

    @Test
    void listEventsWithCategoryUsesFilteredQuery() {
        Event event = eventWithTickets("Marathon", new EventCategory("Sports"), "500");
        when(eventRepository.findByActiveTrueAndCategoryNameIgnoreCaseOrderByEventDateAsc("Sports"))
                .thenReturn(List.of(event));

        List<EventSummaryResponse> result = eventService.listEvents(" Sports ");

        assertThat(result).extracting(EventSummaryResponse::title).containsExactly("Marathon");
        verify(eventRepository).findByActiveTrueAndCategoryNameIgnoreCaseOrderByEventDateAsc("Sports");
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void getEventReturnsTicketCategoriesSortedByPrice() {
        Event event = eventWithTickets("Rock Night", new EventCategory("Concert"), "2499", "999");
        UUID id = UUID.randomUUID();
        when(eventRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(event));

        EventDetailResponse result = eventService.getEvent(id);

        assertThat(result.title()).isEqualTo("Rock Night");
        assertThat(result.ticketCategories()).extracting(TicketCategoryResponse::price)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("999"), new BigDecimal("2499"));
        assertThat(result.ticketCategories().get(0).availableSeats()).isEqualTo(10);
    }

    @Test
    void getEventThrowsWhenMissingOrInactive() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eventWithoutTicketCategoriesHasNullPriceRange() {
        Event event = eventWithTickets("Free Meetup", new EventCategory("Conference"));
        when(eventRepository.findByActiveTrueOrderByEventDateAsc()).thenReturn(List.of(event));

        List<EventSummaryResponse> result = eventService.listEvents("  ");

        assertThat(result.get(0).minPrice()).isNull();
        assertThat(result.get(0).maxPrice()).isNull();
    }
}
