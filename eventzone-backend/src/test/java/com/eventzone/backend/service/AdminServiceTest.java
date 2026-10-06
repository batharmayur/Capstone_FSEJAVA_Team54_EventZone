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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventCategoryRepository categoryRepository;

    private AdminService service;

    @BeforeEach
    void setUp() {
        service = new AdminService(eventRepository, categoryRepository);
    }

    @Test
    void createCategoryTrimsNameAndRejectsCaseInsensitiveDuplicates() {
        when(categoryRepository.findByNameIgnoreCase("Music")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(EventCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.createCategory(new CategoryRequest("  Music ")).name()).isEqualTo("Music");

        when(categoryRepository.findByNameIgnoreCase("concert")).thenReturn(Optional.of(new EventCategory("Concert")));
        assertThatThrownBy(() -> service.createCategory(new CategoryRequest("concert")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void renamingToItsOwnNameWithDifferentCaseIsAllowedButTakingAnotherNameIsNot() {
        EventCategory concert = new EventCategory("Concert");
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(concert, "id", id);
        EventCategory sports = new EventCategory("Sports");
        ReflectionTestUtils.setField(sports, "id", UUID.randomUUID());
        when(categoryRepository.findById(id)).thenReturn(Optional.of(concert));
        when(categoryRepository.findByNameIgnoreCase("CONCERT")).thenReturn(Optional.of(concert));

        EventCategoryResponse renamed = service.updateCategory(id, new CategoryRequest("CONCERT"));
        assertThat(renamed.name()).isEqualTo("CONCERT");

        when(categoryRepository.findByNameIgnoreCase("Sports")).thenReturn(Optional.of(sports));
        assertThatThrownBy(() -> service.updateCategory(id, new CategoryRequest("Sports")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteCategoryIsBlockedWhileEventsUseIt() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(new EventCategory("Concert")));
        when(eventRepository.existsByCategoryId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteCategory(id)).isInstanceOf(BusinessRuleException.class);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteUnusedCategoryRemovesIt() {
        UUID id = UUID.randomUUID();
        EventCategory category = new EventCategory("Old");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(eventRepository.existsByCategoryId(id)).thenReturn(false);

        service.deleteCategory(id);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteUnknownCategoryIsNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCategory(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setEventActiveTogglesTheFlag() {
        Event event = new Event("Show", "d", LocalDateTime.now().plusDays(5), "V", null, new EventCategory("Concert"));
        UUID id = UUID.randomUUID();
        when(eventRepository.findWithDetailsById(id)).thenReturn(Optional.of(event));

        AdminEventResponse off = service.setEventActive(id, false);
        assertThat(off.active()).isFalse();
        assertThat(event.isActive()).isFalse();
        assertThat(off.organiser()).isNull();

        assertThat(service.setEventActive(id, true).active()).isTrue();
    }
}
