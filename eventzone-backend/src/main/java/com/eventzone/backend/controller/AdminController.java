package com.eventzone.backend.controller;

import com.eventzone.backend.dto.ActiveRequest;
import com.eventzone.backend.dto.AdminEventResponse;
import com.eventzone.backend.dto.CategoryRequest;
import com.eventzone.backend.dto.EventCategoryResponse;
import com.eventzone.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Everything under /api/admin requires the ADMIN role (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Category management and event activation (ADMIN only)")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/events")
    @Operation(summary = "List all events including deactivated ones")
    public List<AdminEventResponse> listEvents() {
        return adminService.listEvents();
    }

    @PutMapping("/events/{id}/active")
    @Operation(summary = "Activate or deactivate an event")
    public AdminEventResponse setEventActive(@PathVariable UUID id, @Valid @RequestBody ActiveRequest request) {
        return adminService.setEventActive(id, request.active());
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an event category")
    public EventCategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return adminService.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Rename an event category")
    public EventCategoryResponse updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return adminService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a category not used by any event")
    public void deleteCategory(@PathVariable UUID id) {
        adminService.deleteCategory(id);
    }
}
