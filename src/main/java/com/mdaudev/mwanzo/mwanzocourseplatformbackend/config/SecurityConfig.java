package com.mdaudev.mwanzo.mwanzocourseplatformbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration for Mwanzo Course Platform
 *
 * Purpose: Configure Spring Security for the application
 * Status: Development mode - permits all requests temporarily
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures HTTP security for the application.
     *
     * IMPORTANT: This is a TEMPORARY configuration for development.
     * Before production, we will implement:
     * - JWT authentication
     * - Role-based access control (RBAC)
     * - CSRF protection for state-changing operations
     * - Secure password encoding
     *
     * @param http HttpSecurity object to configure
     * @return SecurityFilterChain with security rules
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF temporarily (we'll enable it later with proper token handling)
                .csrf(AbstractHttpConfigurer::disable)

                // Permit all requests without authentication (TEMPORARY)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}