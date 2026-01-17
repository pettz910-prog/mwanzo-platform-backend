package com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.User;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * User Repository
 *
 * Data access layer for User entity.
 * Provides authentication and user management queries.
 *
 * Microservices-Ready:
 * - Can be extracted to "User Service" independently
 * - Other services query users by UUID only
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email (used for login).
     *
     * @param email User's email address
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by email (case-insensitive).
     * Useful for registration validation.
     *
     * @param email User's email address
     * @return Optional containing user if found
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if email already exists (case-insensitive).
     * Used during registration to prevent duplicates.
     *
     * @param email Email to check
     * @return true if email exists
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find user by email verification token.
     * Used during email verification flow.
     *
     * @param token Verification token
     * @return Optional containing user if found
     */
    Optional<User> findByEmailVerificationToken(String token);

    /**
     * Find user by password reset token.
     * Used during forgot password flow.
     *
     * @param token Password reset token
     * @return Optional containing user if found
     */
    Optional<User> findByPasswordResetToken(String token);

    /**
     * Count users by role.
     * Used for analytics/statistics.
     *
     * @param role User role
     * @return Number of users with specified role
     */
    long countByRole(UserRole role);

    /**
     * Find all active users by role.
     * Used for admin user management.
     *
     * @param role User role
     * @return List of active users
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    java.util.List<User> findActiveUsersByRole(@Param("role") UserRole role);

    /**
     * Count total active users.
     *
     * @return Number of active users
     */
    long countByIsActiveTrue();

    /**
     * Count verified users.
     *
     * @return Number of users with verified emails
     */
    long countByIsEmailVerifiedTrue();
}