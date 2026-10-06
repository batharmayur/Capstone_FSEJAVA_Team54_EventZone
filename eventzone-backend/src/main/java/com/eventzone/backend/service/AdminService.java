package com.eventzone.backend.service;

import com.eventzone.backend.dto.AdminEventResponse;
import com.eventzone.backend.dto.CategoryRequest;
import com.eventzone.backend.dto.EventCategoryResponse;
import com.eventzone.backend.exception.BusinessRuleException;
import com.eventzone.backend.exception.DuplicateResourceException;
import com.eventzone.backend.exception.ResourceNotFoundException;
import com.eventzone.backend.model.Event;
import com.eventzone.backend.model.EventCategory;
import com.eventzone.backend.repository.EventCategoryRepository;
import com.eventzone.backend.repository.EventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Admin operations: event categories and event activation. */
@Service
public class AdminService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;

    public AdminService(EventRepository eventRepository, EventCategoryRepository categoryRepository) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminEventResponse> listEvents() {
        return eventRepository.findAllByOrderByEventDateAsc().stream().map(AdminService::toResponse).toList();
    }

    /** Inactive events disappear from the public catalog and cannot be booked; existing bookings are kept. */
    @Transactional
    public AdminEventResponse setEventActive(UUID eventId, boolean active) {
        Event event = eventRepository.findWithDetailsById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        event.setActive(active);
        return toResponse(event);
    }

    @Transactional
    public EventCategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new DuplicateResourceException("A category named '" + name + "' already exists");
        }
        return toResponse(categoryRepository.save(new EventCategory(name)));
    }

    @Transactional
    public EventCategoryResponse updateCategory(UUID id, CategoryRequest request) {
        EventCategory category = findCategory(id);
        String name = request.name().trim();
        categoryRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("A category named '" + name + "' already exists");
                });
        category.setName(name);
        return toResponse(category);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        EventCategory category = findCategory(id);
        if (eventRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("This category is used by events and cannot be deleted");
        }
        try {
            categoryRepository.delete(category);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleException("This category is used by events and cannot be deleted");
        }
    }

    private EventCategory findCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private static EventCategoryResponse toResponse(EventCategory category) {
        return new EventCategoryResponse(category.getId(), category.getName());
    }

    private static AdminEventResponse toResponse(Event event) {
        return new AdminEventResponse(event.getId(), event.getTitle(), event.getCategory().getName(),
                event.getEventDate(), event.getVenue(), event.isActive(),
                event.getOrganiser() == null ? null : event.getOrganiser().getName());
    }
}
