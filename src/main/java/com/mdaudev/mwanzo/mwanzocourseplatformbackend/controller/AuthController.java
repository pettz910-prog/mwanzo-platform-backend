package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto.AuthResponse;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto.LoginRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto.RegisterRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto.UserDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.AuthenticationService;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST Controller
 *
 * Handles user registration, login, and profile endpoints.
 * All endpoints are public except /me (requires authentication).
 *
 * Base URL: /api/v1/auth
 *
 * Endpoints:
 * - POST   /api/v1/auth/register     - Register new user
 * - POST   /api/v1/auth/login        - Login user
 * - GET    /api/v1/auth/me           - Get current user (requires auth)
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authService;
    private final JwtService jwtService;

    /**
     * Register a new user.
     * Public endpoint - no authentication required.
     *
     * Validation:
     * - Email must be unique
     * - Password minimum 8 characters with uppercase, lowercase, and number
     * - Role must be STUDENT, INSTRUCTOR, or EMPLOYER
     *
     * @param request Registration request with validation
     * @return Authentication response with JWT token
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/v1/auth/register - Registering user: {}", request.getEmail());

        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Registration failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Login user.
     * Public endpoint - no authentication required.
     *
     * Returns JWT token on successful authentication.
     * Token should be included in subsequent requests as:
     * Authorization: Bearer {token}
     *
     * @param request Login request with email and password
     * @param httpRequest HTTP request to extract IP address
     * @return Authentication response with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/v1/auth/login - Login attempt: {}", request.getEmail());

        try {
            // Extract IP address
            String ipAddress = getClientIpAddress(httpRequest);

            AuthResponse response = authService.login(request, ipAddress);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            log.error("Login failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get current authenticated user's profile.
     * Requires valid JWT token in Authorization header.
     *
     * TODO: Extract user from JWT token (SecurityContext) instead of header
     *
     * @param authHeader Authorization header with Bearer token
     * @return Current user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {

        log.info("GET /api/v1/auth/me - Fetching current user");

        // Extract token from "Bearer {token}"
        String token = authHeader.substring(7);

        // Extract email from token
        String email = jwtService.extractEmail(token);

        // Get user profile
        UserDTO user = authService.getUserByEmail(email);

        return ResponseEntity.ok(user);
    }

    /**
     * Extract client IP address from HTTP request.
     * Handles proxy headers (X-Forwarded-For, X-Real-IP).
     *
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        // Handle comma-separated IPs (take first one)
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }
}