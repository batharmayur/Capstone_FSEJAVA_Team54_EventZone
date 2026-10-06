package com.eventzone.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Health")
public class ApiController {

    @GetMapping("/health")
    @Operation(summary = "Service health check")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "eventzone-backend",
                "timestamp", Instant.now().toString()
        );
    }
}
