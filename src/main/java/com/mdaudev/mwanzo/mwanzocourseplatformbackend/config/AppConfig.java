package com.mdaudev.mwanzo.mwanzocourseplatformbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application Configuration
 *
 * Registers common beans used across the application.
 *
 * Beans:
 * - ObjectMapper: For JSON serialization/deserialization
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Configuration
public class AppConfig {

    /**
     * Configure ObjectMapper bean for JSON processing.
     * Used for serializing/deserializing course learning objectives,
     * requirements, and other JSON fields.
     *
     * Configuration:
     * - Registers JavaTimeModule for LocalDateTime support
     * - Disables writing dates as timestamps (uses ISO-8601 instead)
     * - Pretty-prints JSON in development (can be disabled in production)
     *
     * @return Configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register module for Java 8 Date/Time API (LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());

        // Write dates as strings (ISO-8601) instead of timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Pretty print JSON (optional - can disable in production)
        // mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
    }
}