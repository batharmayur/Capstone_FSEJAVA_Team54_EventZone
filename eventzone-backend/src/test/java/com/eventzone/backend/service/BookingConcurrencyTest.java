package com.eventzone.backend.service;

import com.eventzone.backend.dto.BookingRequest;
import com.eventzone.backend.exception.BusinessRuleException;
import com.eventzone.backend.model.Event;
import com.eventzone.backend.model.EventCategory;
import com.eventzone.backend.model.TicketCategory;
import com.eventzone.backend.repository.EventCategoryRepository;
import com.eventzone.backend.repository.EventRepository;
import com.eventzone.backend.repository.TicketCategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the seat count can never be oversold when many bookings arrive at once. */
@SpringBootTest
class BookingConcurrencyTest {

    private static final int SEATS = 3;
    private static final int CONCURRENT_REQUESTS = 10;

    @Autowired
    private BookingService bookingService;
    @Autowired
    private EventCategoryRepository categoryRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    @Test
    void concurrentBookingsNeverExceedAvailableSeats() throws Exception {
        EventCategory category = categoryRepository.save(new EventCategory("Concurrency-" + UUID.randomUUID()));
        Event event = new Event("Limited Show", "desc", LocalDateTime.now().plusDays(400), "Arena", null, category);
        event.getTicketCategories().add(new TicketCategory(event, "General", new BigDecimal("100"), SEATS));
        event = eventRepository.save(event);
        UUID ticketId = event.getTicketCategories().get(0).getId();

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    bookingService.createBooking("user1@eventzone.com", new BookingRequest(ticketId, 1));
                    return true;
                } catch (BusinessRuleException soldOut) {
                    return false;
                }
            }));
        }
        start.countDown();

        int succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get(30, TimeUnit.SECONDS)) {
                succeeded++;
            }
        }
        pool.shutdown();

        assertThat(succeeded).isEqualTo(SEATS);
        assertThat(ticketCategoryRepository.findAvailableSeats(ticketId)).isZero();
    }
}
