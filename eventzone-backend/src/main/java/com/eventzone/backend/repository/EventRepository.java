package com.eventzone.backend.repository;

import com.eventzone.backend.model.Event;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @EntityGraph(attributePaths = {"category", "ticketCategories"})
    Optional<Event> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = {"category", "organiser"})
    Optional<Event> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"category", "ticketCategories"})
    List<Event> findByOrganiserEmailOrderByEventDateAsc(String email);

    @EntityGraph(attributePaths = {"category", "organiser"})
    List<Event> findAllByOrderByEventDateAsc();

    boolean existsByCategoryId(UUID categoryId);

    @EntityGraph(attributePaths = {"category", "ticketCategories"})
    List<Event> findByActiveTrueOrderByEventDateAsc();

    @EntityGraph(attributePaths = {"category", "ticketCategories"})
    List<Event> findByActiveTrueAndCategoryNameIgnoreCaseOrderByEventDateAsc(String categoryName);
}
