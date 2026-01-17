package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Authentication Response DTO
 *
 * Response returned after successful login or registration.
 * Contains JWT token and basic user information.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    /**
     * JWT access token.
     * Include in Authorization header: "Bearer {token}"
     */
    private String token;

    /**
     * Token type (always "Bearer").
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * User ID.
     */
    private UUID userId;

    /**
     * User's full name.
     */
    private String name;

    /**
     * User's email.
     */
    private String email;

    /**
     * User's role (STUDENT, INSTRUCTOR, EMPLOYER, ADMIN).
     */
    private String role;

    /**
     * Whether email is verified.
     */
    private Boolean isEmailVerified;

    /**
     * Profile picture URL.
     */
    private String profilePictureUrl;
}