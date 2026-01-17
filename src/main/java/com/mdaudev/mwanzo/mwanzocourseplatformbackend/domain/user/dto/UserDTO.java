package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User DTO
 *
 * Public user information DTO.
 * Excludes sensitive fields like password, tokens, etc.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String profilePictureUrl;
    private String role;
    private Boolean isEmailVerified;
    private Boolean isActive;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}