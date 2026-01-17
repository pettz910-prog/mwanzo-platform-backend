package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.User;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.UserRole;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto.AuthResponse;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto.LoginRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto.RegisterRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto.UserDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.exception.ResourceNotFoundException;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Authentication Service
 *
 * Handles user registration, login, and authentication logic.
 * Implements Spring Security's UserDetailsService for authentication.
 *
 * Microservices-Ready:
 * - Self-contained authentication logic
 * - Can be extracted to "Auth Service" independently
 * - Issues JWT tokens for stateless authentication
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Load user by username (email) for Spring Security.
     *
     * @param email User's email
     * @return UserDetails
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    /**
     * Register a new user.
     *
     * @param request Registration request
     * @return Authentication response with JWT token
     * @throws IllegalArgumentException if email already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            log.error("Registration failed: Email already exists - {}", request.getEmail());
            throw new IllegalArgumentException("Email already registered");
        }

        // Create user entity
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(UserRole.valueOf(request.getRole()))
                .isEmailVerified(false)
                .isActive(true)
                .isLocked(false)
                .failedLoginAttempts(0)
                .build();

        // Save user
        User savedUser = userRepository.save(user);

        log.info("User registered successfully with ID: {}", savedUser.getId());

        // Generate JWT token
        String token = jwtService.generateToken(savedUser);

        // Return authentication response
        return buildAuthResponse(savedUser, token);
    }

    /**
     * Login user.
     *
     * @param request Login request
     * @param ipAddress Client IP address
     * @return Authentication response with JWT token
     * @throws BadCredentialsException if credentials are invalid
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("Login failed: User not found - {}", request.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        // Check if account is locked
        if (user.getIsLocked()) {
            log.error("Login failed: Account is locked - {}", request.getEmail());
            throw new BadCredentialsException("Account is locked. Please contact support.");
        }

        // Check if account is active
        if (!user.getIsActive()) {
            log.error("Login failed: Account is inactive - {}", request.getEmail());
            throw new BadCredentialsException("Account is inactive. Please contact support.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.error("Login failed: Invalid password - {}", request.getEmail());

            // Record failed login attempt
            user.recordFailedLogin();
            userRepository.save(user);

            throw new BadCredentialsException("Invalid email or password");
        }

        // Record successful login
        user.recordSuccessfulLogin(ipAddress);
        userRepository.save(user);

        log.info("User logged in successfully: {}", user.getId());

        // Generate JWT token
        String token = jwtService.generateToken(user);

        // Return authentication response
        return buildAuthResponse(user, token);
    }

    /**
     * Get user by ID.
     *
     * @param userId User UUID
     * @return User DTO
     * @throws ResourceNotFoundException if user not found
     */
    public UserDTO getUserById(UUID userId) {
        log.debug("Fetching user by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return mapToUserDTO(user);
    }

    /**
     * Get current user by email.
     *
     * @param email User's email
     * @return User DTO
     * @throws ResourceNotFoundException if user not found
     */
    public UserDTO getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return mapToUserDTO(user);
    }

    /**
     * Build authentication response from user and token.
     *
     * @param user User entity
     * @param token JWT token
     * @return Authentication response
     */
    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isEmailVerified(user.getIsEmailVerified())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }

    /**
     * Map User entity to UserDTO.
     *
     * @param user User entity
     * @return User DTO
     */
    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profilePictureUrl(user.getProfilePictureUrl())
                .role(user.getRole().name())
                .isEmailVerified(user.getIsEmailVerified())
                .isActive(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}