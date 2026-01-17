package com.mdaudev.mwanzo.mwanzocourseplatformbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Application Configuration
 *
 * Registers common beans used across the application.
 *
 * Beans:
 * - ObjectMapper: For JSON serialization/deserialization
 * - PasswordEncoder: For secure password hashing
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
     * @return Configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Configure PasswordEncoder bean for secure password hashing.
     * Uses BCrypt with strength 12 (2^12 = 4096 rounds).
     *
     * BCrypt benefits:
     * - Automatically salts passwords
     * - Computationally expensive (slows brute force attacks)
     * - Industry standard for password hashing
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}