package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 *
 * Purpose: Provides basic health check endpoint to verify application is running
 * Endpoint: GET /api/health
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/api")
public class HealthCheckController {

    /**
     * Health check endpoint.
     * Returns application status and basic information.
     *
     * @return ResponseEntity containing health status information
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("application", "Mwanzo Skills Campus Platform");
        response.put("version", "1.0.0");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Application is running successfully!");

        return ResponseEntity.ok(response);
    }
}