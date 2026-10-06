package com.eventzone.backend.repository;

import com.eventzone.backend.model.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventCategoryRepository extends JpaRepository<EventCategory, UUID> {

    List<EventCategory> findAllByOrderByNameAsc();

    Optional<EventCategory> findByNameIgnoreCase(String name);
}
