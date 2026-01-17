package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * User Entity
 *
 * Core user table for authentication and authorization.
 * Implements Spring Security's UserDetails for authentication.
 *
 * Database Table: users
 *
 * Business Rules:
 * - Email must be unique across platform
 * - Password stored as BCrypt hash (never plaintext)
 * - Users can have one primary role (extensible to multiple later)
 * - Email verification required before full access
 * - Accounts can be locked or disabled by admin
 *
 * Microservices-Ready:
 * - Self-contained authentication logic
 * - Can be extracted to "User Service" independently
 * - Other services reference users by UUID only
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_status", columnList = "isActive")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * User's full name.
     */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * User's email address (used for login).
     * Must be unique across platform.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * User's password (stored as BCrypt hash).
     * Never stored or transmitted in plaintext.
     */
    @NotBlank(message = "Password is required")
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * User's phone number (optional, used for M-Pesa).
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * User's profile picture URL.
     * Stored in S3, URL saved here.
     */
    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    /**
     * User's primary role.
     * Determines access permissions.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.STUDENT;

    /**
     * Whether user has verified their email.
     */
    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    /**
     * Email verification token.
     * Sent in verification email, cleared after verification.
     */
    @Column(name = "email_verification_token", length = 100)
    private String emailVerificationToken;

    /**
     * When verification token expires.
     */
    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;

    /**
     * Password reset token (for forgot password flow).
     */
    @Column(name = "password_reset_token", length = 100)
    private String passwordResetToken;

    /**
     * When password reset token expires.
     */
    @Column(name = "password_reset_expiry")
    private LocalDateTime passwordResetExpiry;

    /**
     * Whether user account is active.
     * Admin can deactivate accounts for policy violations.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Whether account is locked (too many failed login attempts).
     */
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    /**
     * Number of failed login attempts.
     * Reset to 0 on successful login.
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    /**
     * When account was locked.
     */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    /**
     * Last successful login timestamp.
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * Last login IP address.
     */
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    /**
     * Timestamp when user registered.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when user record was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==========================================
    // Spring Security UserDetails Implementation
    // ==========================================

    /**
     * Get user's granted authorities (roles).
     * Spring Security uses this for authorization.
     *
     * @return Collection of authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + this.role.name())
        );
    }

    /**
     * Get username for Spring Security.
     * We use email as username.
     *
     * @return User's email
     */
    @Override
    public String getUsername() {
        return this.email;
    }

    /**
     * Check if account is not expired.
     * Currently always returns true (we don't expire accounts).
     *
     * @return true if account is not expired
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Check if account is not locked.
     *
     * @return true if account is not locked
     */
    @Override
    public boolean isAccountNonLocked() {
        return !this.isLocked;
    }

    /**
     * Check if credentials are not expired.
     * Currently always returns true (passwords don't expire).
     *
     * @return true if credentials are not expired
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Check if account is enabled.
     *
     * @return true if account is active
     */
    @Override
    public boolean isEnabled() {
        return this.isActive;
    }

    // ==========================================
    // Business Logic Methods
    // ==========================================

    /**
     * Record successful login.
     * Updates last login timestamp and IP, resets failed attempts.
     *
     * @param ipAddress IP address of login
     */
    public void recordSuccessfulLogin(String ipAddress) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
        this.failedLoginAttempts = 0;

        if (this.isLocked) {
            this.isLocked = false;
            this.lockedAt = null;
        }
    }

    /**
     * Record failed login attempt.
     * Increments failed attempts counter, locks account after 5 failures.
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;

        if (this.failedLoginAttempts >= 5) {
            this.isLocked = true;
            this.lockedAt = LocalDateTime.now();
        }
    }

    /**
     * Verify email.
     * Marks email as verified and clears verification token.
     */
    public void verifyEmail() {
        this.isEmailVerified = true;
        this.emailVerificationToken = null;
        this.verificationTokenExpiry = null;
    }

    /**
     * Check if user has a specific role.
     *
     * @param role Role to check
     * @return true if user has the role
     */
    public boolean hasRole(UserRole role) {
        return this.role == role;
    }
}