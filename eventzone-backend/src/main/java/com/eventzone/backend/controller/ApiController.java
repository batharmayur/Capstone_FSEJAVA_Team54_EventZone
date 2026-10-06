package com.eventzone.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "eventzone-backend",
                "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/projects")
    public Map<String, Object> projects() {
        return Map.of(
                "total", 3,
                "projects", List.of(
                        Map.of("id", "p1", "title", "Rock Night 2025", "status", "IN_REVIEW", "progress", 72),
                        Map.of("id", "p2", "title", "Championship Weekend", "status", "APPROVED", "progress", 84),
                        Map.of("id", "p3", "title", "AI Product Meetup", "status", "DRAFT", "progress", 38)
                )
        );
    }
}
