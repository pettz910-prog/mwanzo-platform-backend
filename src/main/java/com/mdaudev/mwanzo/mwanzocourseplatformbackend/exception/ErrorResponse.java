package com.mdaudev.mwanzo.mwanzocourseplatformbackend.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Error Response DTO
 *
 * Standardized error response format for all API errors.
 * Provides consistent structure for error handling across the application.
 *
 * Example JSON response:
 * {
 *   "timestamp": "2026-01-09T16:45:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Course not found with id: '123e4567-e89b-12d3-a456-426614174000'",
 *   "path": "/api/v1/courses/123e4567-e89b-12d3-a456-426614174000"
 * }
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    /**
     * Timestamp when error occurred.
     */
    private LocalDateTime timestamp;

    /**
     * HTTP status code (e.g., 404, 500).
     */
    private int status;

    /**
     * HTTP status text (e.g., "Not Found", "Internal Server Error").
     */
    private String error;

    /**
     * Human-readable error message.
     */
    private String message;

    /**
     * Request path that caused the error.
     */
    private String path;

    /**
     * Field-level validation errors (for 400 Bad Request).
     * Optional - only present for validation failures.
     *
     * Example:
     * {
     *   "title": "Course title is required",
     *   "price": "Price cannot be negative"
     * }
     */
    private Map<String, String> fieldErrors;
}